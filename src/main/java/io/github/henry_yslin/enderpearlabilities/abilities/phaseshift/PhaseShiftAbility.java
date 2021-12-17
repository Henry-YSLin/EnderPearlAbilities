package io.github.henry_yslin.enderpearlabilities.abilities.phaseshift;

import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henry_yslin.enderpearlabilities.managers.interactionlock.InteractionLockManager;
import io.github.henry_yslin.enderpearlabilities.managers.voidspace.VoidSpaceManager;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.EntityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.PlayerUtils;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.atomic.AtomicBoolean;

public class PhaseShiftAbility extends Ability {

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        super.setConfigDefaults(config);
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 50);
        config.addDefault("cooldown", 400);
    }

    public PhaseShiftAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("phase-shift")
                .name("Phase Shift")
                .origin("Titanfall")
                .description("Become invulnerable and invisible by entering an alternate dimension.")
                .usage("Right click with an ender pearl to activate the ability. Right click again to exit early. You may not interact with anything while the ability is active.")
                .activation(ActivationHand.OffHand);

        if (config != null)
            builder
                    .chargeUp(config.getInt("charge-up"))
                    .duration(config.getInt("duration"))
                    .cooldown(config.getInt("cooldown"));

        info = builder.build();
    }

    @Override
    public AbilityInfo getInfo() {
        return info;
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);

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
        cooldown.setCooldown(info.cooldown);
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (abilityActive.get())
            if (player.getWorld().equals(event.getEntity().getWorld()))
                EntityUtils.destroyEntityForPlayer(event.getEntity(), player);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation, true)) return;

        event.setCancelled(true);

        if (player.getName().equals(ownerName) && abilityActive.get()) {
            abilityActive.set(false);
            return;
        }

        if (InteractionLockManager.getInstance().isInteractionLocked(player)) return;

        if (cooldown.isCoolingDown()) return;
        if (chargingUp.get()) return;

        new FunctionChain(
                next -> {
                    PlayerUtils.consumeEnderPearl(player);
                    next.run();
                },
                next -> AbilityUtils.chargeUpSequence(this, player, info.chargeUp, chargingUp, next),
                next -> {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.7f, 2f);
                    abilityActive.set(true);

                    VoidSpaceManager.getInstance().enterVoid(player);
                    for (Entity entity : player.getWorld().getEntities())
                        if (!entity.equals(player))
                            EntityUtils.destroyEntityForPlayer(entity, player);
                    player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 5, 0.5, 1, 0.5, 0.02, null, true);

                    next.run();
                },
                next -> new AbilityRunnable() {
                    BossBar bossbar;

                    @Override
                    protected synchronized void start() {
                        bossbar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.name, BarColor.PURPLE, BarStyle.SOLID, BarFlag.CREATE_FOG, BarFlag.DARKEN_SKY);
                        bossbar.addPlayer(player);
                    }

                    @Override
                    protected synchronized void tick() {
                        if (!abilityActive.get() || !player.isValid()) {
                            cancel();
                        }
                        bossbar.setProgress(count / (double) info.duration);
                    }

                    @Override
                    protected synchronized void end() {
                        bossbar.removeAll();
                        next.run();
                    }
                }.runTaskRepeated(this, 0, 1, info.duration),
                next -> {
                    abilityActive.set(false);

                    Location loc = player.getLocation().clone();
                    GameMode gameMode = player.getGameMode();
                    boolean isFlying = player.isFlying();
                    boolean isGliding = player.isGliding();
                    player.setGameMode(GameMode.SPECTATOR);
                    player.teleport(player.getLocation().add(300, 300, 300));
                    AbilityUtils.delay(this, 10, () -> {
                        player.teleport(loc);
                        VoidSpaceManager.getInstance().exitVoid(player);
                        player.setGameMode(gameMode);
                        player.setFlying(isFlying);
                        player.setGliding(isGliding);
                        player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 5, 0.5, 1, 0.5, 0.02, null, true);
                    }, true);

                    cooldown.setCooldown(info.cooldown);
                    next.run();
                }
        ).execute();
    }
}
