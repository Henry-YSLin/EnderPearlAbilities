package io.github.henry_yslin.enderpearlabilities.abilities.vantagetactical;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.*;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.managers.abilitylock.AbilityLockManager;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.EntityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Bat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class VantageTacticalAbility extends Ability<VantageTacticalAbilityInfo> {

    static final float ECHO_SPEED = 1f;
    static final float LAUNCH_SPEED = 1f;
    static final float MAX_RANGE = 35f;
    static final float ECHO_ELEVATION = 5f;
    static final float RECALL_RADIUS = 2;

    public VantageTacticalAbility(Plugin plugin, VantageTacticalAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    final AtomicBoolean moving = new AtomicBoolean(false);
    final AtomicReference<LivingEntity> echo = new AtomicReference<>();
    EchoStatusRunnable echoStatusRunnable;

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

    public boolean isMoving() {
        return moving.get();
    }

    public LivingEntity getEchoEntity() {
        return echo.get();
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
        removeEcho();
        if (player != null) {
            setUpPlayer(player);
        }
    }

    private LivingEntity spawnEcho(Location deployLocation) {
        if (deployLocation.getWorld() == null) {
            plugin.getLogger().warning("Trying to spawn Echo with null world.");
            return null;
        }
        removeEcho();
        Bat bat = deployLocation.getWorld().spawn(deployLocation, Bat.class, false, entity -> {
            entity.setGravity(false);
            if (entity.getEquipment() != null)
                entity.getEquipment().setItemInMainHand(new ItemStack(Material.AIR, 0));
            entity.setAware(false);
            entity.setPersistent(true);
            entity.setRemoveWhenFarAway(false);
            entity.setCollidable(false);
            entity.setCustomName(ownerName + "'s Echo");
            entity.setCustomNameVisible(true);
            entity.setInvulnerable(true);
            entity.setAwake(true);
            entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.getCodeName(), ownerName)));
        });
        echo.set(bat);
        return bat;
    }

    private void removeEcho() {
        LivingEntity d = echo.get();
        if (d != null) {
            d.remove();
            echo.set(null);
        }
    }

    private boolean isEchoValid() {
        return echo.get() != null && echo.get().isValid() && echo.get().getWorld().equals(player.getWorld());
    }

    private void setUpPlayer(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }

        abilityActive.set(false);
        chargingUp.set(false);
        moving.set(false);
        cooldown.setCooldown(info.getCooldown());

        if (echoStatusRunnable != null && !echoStatusRunnable.isCancelled())
            echoStatusRunnable.cancel();
        echoStatusRunnable = new EchoStatusRunnable(this, player, echo, chargingUp, abilityActive);
        echoStatusRunnable.runTaskTimer(this, 0, 5);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        removeEcho();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        removeEcho();
    }

    @EventHandler
    public void onEntityDropItem(EntityDropItemEvent event) {
        if (!AbilityUtils.verifyAbilityCouple(this, event.getEntity())) return;
        event.getItemDrop().remove();
    }

    private void warnNotEnoughSpace(Player player) {
        player.sendTitle(" ", ChatColor.LIGHT_PURPLE + "Not enough space", 5, 20, 10);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!event.getEntity().equals(player)) return;
        removeEcho();
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (!event.getPlayer().equals(player)) return;
        removeEcho();
    }

    private Location calculateEchoLocation(Player player) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();
        RayTraceResult result = player.getWorld().rayTraceBlocks(eyeLocation, direction, MAX_RANGE, FluidCollisionMode.ALWAYS, true);
        if (result == null) {
            return eyeLocation.add(direction.multiply(MAX_RANGE));
        } else {
            Location approxLocation = result.getHitPosition().subtract(direction.normalize().multiply(0.5)).toLocation(player.getWorld());
            RayTraceResult verticalResult = player.getWorld().rayTraceBlocks(approxLocation, new Vector(0, 1, 0), ECHO_ELEVATION + 0.5, FluidCollisionMode.ALWAYS, true);
            if (verticalResult == null) {
                return approxLocation.add(0, ECHO_ELEVATION, 0);
            } else {
                return verticalResult.getHitPosition().subtract(new Vector(0, 0.5, 0)).toLocation(player.getWorld());
            }
        }
    }

    private void moveEcho(Location newLocation) {
        new AbilityRunnable() {
            Location lastLocation;
            LivingEntity bat;

            @Override
            protected synchronized void start() {
                bat = echo.get();
                moving.set(true);
                lastLocation = bat.getLocation();
            }

            @Override
            protected synchronized void tick() {
                if (!moving.get()) return;
                if (!isEchoValid()) {
                    cancel();
                    return;
                }
                if (bat.getLocation().distanceSquared(newLocation) < ECHO_SPEED * ECHO_SPEED + 0.3) {
                    cancel();
                    return;
                }
                Vector offset = newLocation.toVector().subtract(bat.getLocation().toVector()).normalize().multiply(ECHO_SPEED);
                lastLocation.setDirection(offset);
                bat.teleport(lastLocation.add(offset));
                bat.setVelocity(offset);
                bat.getWorld().spawnParticle(Particle.PORTAL, bat.getLocation(), 5, 0.1, 0.1, 0.1, 0.02);
                lastLocation = bat.getLocation();
            }

            @Override
            protected synchronized void end() {
                moving.set(false);
                bat.teleport(newLocation);
                bat.setVelocity(new Vector(0, 0, 0));
            }
        }.runTaskRepeated(this, 0, 1, 100);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.getName().equals(ownerName)) return;
        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation(), true)) return;

        event.setCancelled(true);

        if (chargingUp.get()) return;
        if (abilityActive.get()) return;

        if (isEchoValid()) {
            if (player.isSneaking()) {
                if (moving.get()) return;
                Location echoLocation = calculateEchoLocation(player);
                if (echoLocation.toVector().distanceSquared(new Vector(player.getLocation().getX(), echoLocation.getY(), player.getLocation().getZ())) > RECALL_RADIUS * RECALL_RADIUS) {
                    player.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, echoLocation, 5, 0.1, 0.1, 0.1, 0);
                    moveEcho(echoLocation);
                } else {
                    new AbilityRunnable() {
                        BossBar bossbar;
                        double initialDistance;
                        Location lastLocation;
                        LivingEntity bat;

                        @Override
                        protected synchronized void start() {
                            moving.set(true);
                            player.setCooldown(Material.ENDER_PEARL, 20);
                            bossbar = Bukkit.createBossBar("Recalling Echo", BarColor.WHITE, BarStyle.SOLID);
                            bossbar.setProgress(0);
                            bossbar.addPlayer(player);
                            bat = echo.get();
                            initialDistance = player.getLocation().distance(bat.getLocation());
                            lastLocation = bat.getLocation();
                        }

                        @Override
                        protected synchronized void tick() {
                            if (!moving.get()) return;
                            if (!isEchoValid()) {
                                cancel();
                                return;
                            }
                            if (bat.getLocation().distanceSquared(player.getLocation()) < ECHO_SPEED * ECHO_SPEED + 0.3) {
                                cancel();
                                return;
                            }
                            bossbar.setProgress(1 - Math.min(1, player.getLocation().distance(bat.getLocation()) / initialDistance));
                            Vector offset = player.getLocation().toVector().subtract(bat.getLocation().toVector()).normalize().multiply(ECHO_SPEED);
                            lastLocation.setDirection(offset);
                            bat.teleport(lastLocation.add(offset));
                            bat.setVelocity(offset);
                            bat.getWorld().spawnParticle(Particle.PORTAL, bat.getLocation(), 5, 0.1, 0.1, 0.1, 0.02);
                            lastLocation = bat.getLocation();
                        }

                        @Override
                        protected synchronized void end() {
                            bossbar.removeAll();
                            moving.set(false);
                            removeEcho();
                        }
                    }.runTaskRepeated(this, 0, 1, 100);
                }
            } else {
                if (!cooldown.isAbilityUsable()) return;
                if (AbilityLockManager.getInstance().isAbilityLocked(player)) return;

                new FunctionChain(
                        next -> AbilityUtils.chargeUpSequence(this, player, info.getChargeUp(), chargingUp, next, count -> player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.05f, (info.getChargeUp() - count) / (float) info.getChargeUp() * 0.3f)),
                        next -> {
                            if (!isEchoValid()) return;
                            Vector toBat = echo.get().getLocation().subtract(player.getEyeLocation()).toVector();
                            Vector direction = player.getEyeLocation().getDirection();
                            if (toBat.dot(direction) < 0) {
                                player.sendTitle(" ", ChatColor.LIGHT_PURPLE + "Lost line of sight to Echo", 5, 20, 10);
                                return;
                            }
                            RayTraceResult result = player.getWorld().rayTraceBlocks(player.getEyeLocation(), toBat, toBat.length(), FluidCollisionMode.NEVER, true);
                            if (result != null) {
                                player.sendTitle(" ", ChatColor.LIGHT_PURPLE + "Lost line of sight to Echo", 5, 20, 10);
                                return;
                            }

                            AbilityUtils.consumeEnderPearl(this, player);
                            EnderPearlAbilities.getInstance().emitEvent(
                                    EventListener.class,
                                    new AbilityActivateEvent(this),
                                    EventListener::onAbilityActivate
                            );

                            next.run();
                        },
                        next -> new AbilityRunnable() {
                            LivingEntity bat;

                            @Override
                            protected synchronized void start() {
                                abilityActive.set(true);
                                bat = echo.get();
                            }

                            @Override
                            protected synchronized void tick() {
                                if (!abilityActive.get() || !player.isValid()) {
                                    cancel();
                                    return;
                                }
                                if (!isEchoValid()) {
                                    cancel();
                                    return;
                                }
                                Vector offset = bat.getLocation().subtract(player.getLocation()).toVector();
                                if (offset.lengthSquared() < LAUNCH_SPEED * LAUNCH_SPEED + 0.3) {
                                    cancel();
                                    return;
                                }
                                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.05f, 0.3f);
                                offset.normalize().multiply(LAUNCH_SPEED);
                                offset.add(new Vector(0, 0.02 + EntityUtils.getGravity(player), 0));
                                player.setVelocity(offset);
                                player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, player.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.02);
                            }

                            @Override
                            protected synchronized void end() {
                                abilityActive.set(false);
                                cooldown.setCooldown(info.getCooldown());
                                player.setFallDistance(0);
                                next.run();
                            }
                        }.runTaskRepeated(this, 0, 1, info.getDuration())
                ).execute();
            }
        } else {
            moving.set(false);
            Location echoLocation = calculateEchoLocation(player);
            if (echoLocation.toVector().distanceSquared(new Vector(player.getLocation().getX(), echoLocation.getY(), player.getLocation().getZ())) <= RECALL_RADIUS * RECALL_RADIUS) {
                warnNotEnoughSpace(player);
                return;
            }

            player.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, echoLocation, 5, 0.1, 0.1, 0.1, 0);
            spawnEcho(player.getLocation());
            moveEcho(echoLocation);
            cooldown.setCooldown(20);
        }
    }
}
