package io.github.henry_yslin.enderpearlabilities.abilities.wraithultimate;

import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henry_yslin.enderpearlabilities.managers.interactionlock.InteractionLockManager;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.PlayerUtils;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class WraithUltimateAbility extends Ability {

    static final double MAX_DISTANCE = 120;
    static final double REFUNDABLE_DISTANCE = 5;

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        super.setConfigDefaults(config);
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 1200);
        config.addDefault("cooldown", 1200);
    }

    public WraithUltimateAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("wraith-ultimate")
                .name("Dimensional Rift")
                .origin("Apex - Wraith")
                .description("Link two locations with portals, allowing anyone to use them.")
                .usage("Right click with an ender pearl to set the first portal location. Go to the second location and right click again to set the second portal. You have extra buffs while setting portals. Portals have a limited distance and travelling to another dimension consumes extra portal distance.")
                .activation(ActivationHand.MainHand);

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
    AbilityRunnable portal;

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

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation, true)) return;

        if (player.getName().equals(ownerName) && abilityActive.get()) {
            abilityActive.set(false);
            event.setCancelled(true);
            return;
        }

        if (InteractionLockManager.getInstance().isInteractionLocked(player)) return;

        event.setCancelled(true);

        if (abilityActive.get()) return;
        if (chargingUp.get()) return;
        if (cooldown.isCoolingDown()) return;

        Location[] locations = new Location[2];

        new FunctionChain(
                next -> {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.3f, 2);
                    PlayerUtils.consumeEnderPearl(player);
                    next.run();
                },
                next -> AbilityUtils.chargeUpSequence(this, player, info.chargeUp, chargingUp, next),
                next -> {
                    abilityActive.set(true);
                    player.addPotionEffect(PotionEffectType.NIGHT_VISION.createEffect(1000000, 0));
                    player.addPotionEffect(PotionEffectType.CONDUIT_POWER.createEffect(1000000, 0));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1000000, 1, true, true));
                    next.run();
                },
                next -> new AbilityRunnable() {
                    BossBar bossbar;
                    Location lastLocation;
                    double distanceRemaining = MAX_DISTANCE;
                    boolean setPortal = false;

                    @Override
                    protected synchronized void start() {
                        bossbar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.name, BarColor.BLUE, BarStyle.SOLID);
                        bossbar.addPlayer(player);
                        lastLocation = player.getLocation();
                        locations[0] = player.getLocation().add(0, 1, 0);
                    }

                    @Override
                    protected synchronized void tick() {
                        if (!player.isValid()) {
                            setPortal = false;
                            cancel();
                            return;
                        }
                        if (!abilityActive.get()) {
                            setPortal = true;
                            cancel();
                            return;
                        }
                        double distance;
                        if (player.getLocation().getWorld() == lastLocation.getWorld())
                            distance = player.getLocation().toVector().setY(0).distance(lastLocation.toVector().setY(0));
                        else
                            distance = 20;
                        distanceRemaining -= distance;
                        lastLocation = player.getLocation();
                        if (distanceRemaining <= 0) {
                            setPortal = true;
                            cancel();
                            return;
                        }
                        player.getWorld().spawnParticle(Particle.DRAGON_BREATH, locations[0], 5, 0.05, 0.05, 0.05, 0.005, null, true);
                        player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation().add(0, 1, 0), 2, 0.1, 0.1, 0.1, 0);
                        bossbar.setProgress(distanceRemaining / MAX_DISTANCE);
                        if (MAX_DISTANCE - distanceRemaining > REFUNDABLE_DISTANCE)
                            bossbar.setColor(BarColor.PURPLE);
                    }

                    @Override
                    protected synchronized void end() {
                        bossbar.removeAll();
                        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                        player.removePotionEffect(PotionEffectType.CONDUIT_POWER);
                        player.removePotionEffect(PotionEffectType.SPEED);
                        abilityActive.set(false);
                        if (setPortal) {
                            if (MAX_DISTANCE - distanceRemaining <= REFUNDABLE_DISTANCE)
                                return;
                            cooldown.setCooldown(info.cooldown);
                            locations[1] = player.getLocation().add(0, 1, 0);
                            next.run();
                        }
                    }
                }.runTaskRepeated(this, 0, 1, info.duration),
                next -> {
                    if (portal != null && !portal.isCancelled())
                        portal.cancel();
                    (portal = new AbilityRunnable() {
                        final List<Entity> justTeleported = new ArrayList<>();
                        Location from;
                        Location to;
                        BoundingBox fromBox;
                        BoundingBox toBox;

                        @Override
                        protected void start() {
                            from = locations[0];
                            to = locations[1];
                            fromBox = BoundingBox.of(locations[0], 0.5, 1, 0.5);
                            toBox = BoundingBox.of(locations[1], 0.5, 1, 0.5);
                            justTeleported.add(player);
                        }

                        @Override
                        protected void tick() {
                            Objects.requireNonNull(from.getWorld());
                            Objects.requireNonNull(to.getWorld());
                            from.getWorld().spawnParticle(Particle.DRAGON_BREATH, from, 10, 0.3, 1, 0.3, 0.01, null, true);
                            to.getWorld().spawnParticle(Particle.DRAGON_BREATH, to, 10, 0.3, 1, 0.3, 0.01, null, true);

                            for (Entity entity : from.getWorld().getNearbyEntities(fromBox, entity -> entity instanceof LivingEntity && !justTeleported.contains(entity))) {
                                from.getWorld().playSound(from, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0.5f);
                                to.getWorld().playSound(to, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0.5f);
                                entity.teleport(to.clone().subtract(0, 0.99, 0));
                                justTeleported.add(entity);
                                entity.setFallDistance(0);
                                entity.setVelocity(new Vector());
                            }

                            for (Entity entity : to.getWorld().getNearbyEntities(toBox, entity -> entity instanceof LivingEntity && !justTeleported.contains(entity))) {
                                from.getWorld().playSound(from, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0.5f);
                                to.getWorld().playSound(to, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0.5f);
                                entity.teleport(from.clone().subtract(0, 0.99, 0));
                                justTeleported.add(entity);
                                entity.setFallDistance(0);
                                entity.setVelocity(new Vector());
                            }

                            justTeleported.removeIf(entity -> !toBox.overlaps(entity.getBoundingBox()) && !fromBox.overlaps(entity.getBoundingBox()));
                        }

                        @Override
                        protected void end() {
                            super.end();
                        }
                    }).runTaskRepeated(this, 0, 1, info.duration);
                }
        ).execute();
    }
}
