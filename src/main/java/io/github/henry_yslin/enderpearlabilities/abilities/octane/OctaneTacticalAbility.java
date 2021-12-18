package io.github.henry_yslin.enderpearlabilities.abilities.octane;

import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.atomic.AtomicBoolean;

public class OctaneTacticalAbility extends Ability<OctaneTacticalAbilityInfo> {

    public OctaneTacticalAbility(Plugin plugin, OctaneTacticalAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    SwiftMendRunnable swiftMendRunnable;

    @Override
    public boolean isActive() {
        return abilityActive.get();
    }

    @Override
    public boolean isChargingUp() {
        return chargingUp.get();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        super.onPlayerJoin(event);
        Player player = event.getPlayer();
        if (player.getName().equals(ownerName)) {
            setUpPlayer(player);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (player != null) {
            setUpPlayer(player);
        }
    }

    private void setUpPlayer(Player player) {
        chargingUp.set(false);
        abilityActive.set(false);
        cooldown.setCooldown(info.getCooldown());

        if (swiftMendRunnable != null && !swiftMendRunnable.isCancelled())
            swiftMendRunnable.cancel();
        swiftMendRunnable = new SwiftMendRunnable(player);
        swiftMendRunnable.runTaskTimer(this, 0, 20);
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventHandler
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        if (!abilityActive.get()) return;
        if (event.isSprinting())
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1000000, 2, true, true));
        else
            player.removePotionEffect(PotionEffectType.SPEED);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;

        event.setCancelled(true);

        if (abilityActive.get()) return;
        if (chargingUp.get()) return;
        if (cooldown.isCoolingDown()) return;

        new FunctionChain(
                next -> {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 2);
                    if (Math.random() < 0.25)
                        PlayerUtils.consumeEnderPearl(player);
                    player.setHealth(Math.max(1, player.getHealth() - 4));
                    next.run();
                },
                next -> AbilityUtils.chargeUpSequence(this, player, info.getChargeUp(), chargingUp, next),
                next -> {
                    abilityActive.set(true);
                    player.addPotionEffect(PotionEffectType.NIGHT_VISION.createEffect(10, 0));
                    player.addPotionEffect(PotionEffectType.CONDUIT_POWER.createEffect(10, 0));
                    player.addPotionEffect(PotionEffectType.JUMP.createEffect(info.getDuration(), 1));
                    if (player.isSprinting())
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1000000, 2, true, true));
                    next.run();
                },
                next -> new AbilityRunnable() {
                    BossBar bossbar;

                    @Override
                    protected synchronized void start() {
                        bossbar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.getName(), BarColor.PURPLE, BarStyle.SOLID);
                        bossbar.addPlayer(player);
                    }

                    @Override
                    protected synchronized void tick() {
                        if (!abilityActive.get() || !player.isValid()) {
                            cancel();
                        }
                        bossbar.setProgress(count / (double) info.getDuration());
                        if (player.hasPotionEffect(PotionEffectType.SLOW))
                            player.removePotionEffect(PotionEffectType.SLOW);
                        player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1, 0), 2, 0.1, 0.1, 0.1, 0);
                    }

                    @Override
                    protected synchronized void end() {
                        bossbar.removeAll();
                        next.run();
                    }
                }.runTaskRepeated(this, 0, 1, info.getDuration()),
                next -> {
                    for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                        onlinePlayer.showPlayer(plugin, player);
                    }
                    abilityActive.set(false);
                    player.removePotionEffect(PotionEffectType.SPEED);
                    player.removePotionEffect(PotionEffectType.JUMP);
                    cooldown.setCooldown(info.getCooldown());
                    next.run();
                }
        ).execute();
    }
}
