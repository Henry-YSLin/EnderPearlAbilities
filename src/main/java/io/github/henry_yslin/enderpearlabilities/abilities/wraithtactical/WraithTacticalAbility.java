package io.github.henry_yslin.enderpearlabilities.abilities.wraithtactical;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityCooldown;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.SingleUseCooldown;
import io.github.henry_yslin.enderpearlabilities.abilities.wraithultimate.WraithUltimateAbility;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.managers.abilitylock.AbilityLockManager;
import io.github.henry_yslin.enderpearlabilities.managers.voidspace.VoidSpaceManager;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class WraithTacticalAbility extends Ability<WraithTacticalAbilityInfo> {

    static final int CANCEL_DELAY = 10;

    public WraithTacticalAbility(Plugin plugin, WraithTacticalAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);

        subListeners.add(new VoicesFromTheVoidListener(plugin, this));
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    final AtomicBoolean cancelAbility = new AtomicBoolean(false);

    @Override
    protected AbilityCooldown createCooldown() {
        return new SingleUseCooldown(this, player);
    }

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
    }

    public void cancelAbility() {
        if (abilityActive.get()) {
            abilityActive.set(false);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation(), true)) return;

        event.setCancelled(true);

        if (player.getName().equals(ownerName) && abilityActive.get()) {
            cancelAbility.set(true);
            return;
        }

        if (AbilityLockManager.getInstance().isAbilityLocked(player)) return;

        if (!cooldown.isAbilityUsable()) return;
        if (chargingUp.get()) return;

        Optional<WraithUltimateAbility> ultimate = EnderPearlAbilities.getInstance().getAbilities().stream()
                .filter(ability -> ability instanceof WraithUltimateAbility && ability.getOwnerName().equals(ownerName))
                .findFirst()
                .map(ability -> (WraithUltimateAbility) ability);

        boolean tmpQuickActivation = false;
        if (ultimate.isPresent()) {
            if (ultimate.get().isActive()) {
                tmpQuickActivation = true;
            }
        }
        boolean quickActivation = tmpQuickActivation;

        new FunctionChain(
                next -> {
                    AbilityUtils.consumeEnderPearl(this, player);
                    EnderPearlAbilities.getInstance().emitEvent(
                            EventListener.class,
                            new AbilityActivateEvent(this),
                            EventListener::onAbilityActivate
                    );
                    if (!quickActivation)
                        player.addPotionEffect(PotionEffectType.SLOW.createEffect(info.getChargeUp(), 2));
                    next.run();
                },
                next -> AbilityUtils.chargeUpSequence(this, player, info.getChargeUp() / (quickActivation ? 5 : 1), chargingUp, next),
                next -> {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.7f, 0.8f);
                    cancelAbility.set(false);
                    abilityActive.set(true);

                    VoidSpaceManager.getInstance().enterVoid(player);

                    next.run();
                },
                next -> new AbilityRunnable() {
                    BossBar bossbar;
                    int cancelTick = CANCEL_DELAY;

                    @Override
                    protected synchronized void start() {
                        bossbar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.getName(), BarColor.PURPLE, BarStyle.SOLID, BarFlag.CREATE_FOG, BarFlag.DARKEN_SKY);
                        bossbar.addPlayer(player);
                    }

                    @Override
                    protected synchronized void tick() {
                        if (!abilityActive.get() || !player.isValid()) {
                            cancel();
                        }
                        if (cancelAbility.get()) {
                            cancelTick--;
                            player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 5, 0.5, 1, 0.5, 0.02, null, true);
                            if (cancelTick <= 0) abilityActive.set(false);
                        }
                        bossbar.setProgress(count / (double) info.getDuration());
                        player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation().add(0, 1, 0), 10, 0.5, 0.7, 0.5, 0.02);
                        if (count < CANCEL_DELAY)
                            player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 5, 0.5, 1, 0.5, 0.02, null, true);
                    }

                    @Override
                    protected synchronized void end() {
                        bossbar.removeAll();
                        next.run();
                    }
                }.runTaskRepeated(this, 0, 1, info.getDuration()),
                next -> {
                    abilityActive.set(false);
                    cancelAbility.set(false);

                    VoidSpaceManager.getInstance().exitVoid(player);

                    if (ultimate.isPresent())
                        if (ultimate.get().isActive()) {
                            player.addPotionEffect(PotionEffectType.NIGHT_VISION.createEffect(Integer.MAX_VALUE, 0));
                            player.addPotionEffect(PotionEffectType.CONDUIT_POWER.createEffect(Integer.MAX_VALUE, 0));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, true, true));
                        }

                    cooldown.setCooldown(info.getCooldown());
                    next.run();
                }
        ).execute();
    }
}
