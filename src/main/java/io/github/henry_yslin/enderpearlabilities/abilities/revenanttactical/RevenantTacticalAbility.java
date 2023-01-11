package io.github.henry_yslin.enderpearlabilities.abilities.revenanttactical;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityCooldown;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.MultipleChargeCooldown;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.managers.abilitylock.AbilityLockManager;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.ProjectileUtils;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.concurrent.atomic.AtomicBoolean;

public class RevenantTacticalAbility extends Ability<RevenantTacticalAbilityInfo> {

    static final int PROJECTILE_LIFETIME = 100;
    static final double PROJECTILE_SPEED = 3;
    static final boolean PROJECTILE_GRAVITY = true;
    static final double ORB_RADIUS = 2;

    public RevenantTacticalAbility(Plugin plugin, RevenantTacticalAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);

        subListeners.add(new StalkerListener(plugin, this));
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);

    @Override
    protected AbilityCooldown createCooldown() {
        return new MultipleChargeCooldown(this, player, 2);
    }

    @Override
    public boolean isActive() {
        return false;
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

    @Override
    public void onDisable() {
        super.onDisable();
    }

    private void setUpPlayer(Player player) {
        cooldown.setCooldown(info.getCooldown());
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;

        event.setCancelled(true);

        if (!cooldown.isAbilityUsable()) return;
        if (chargingUp.get()) return;

        new FunctionChain(
                next -> AbilityUtils.chargeUpSequence(this, player, info.getChargeUp(), chargingUp, next),
                next -> {
                    Projectile projectile = AbilityUtils.fireProjectile(this, player, null, PROJECTILE_LIFETIME, PROJECTILE_SPEED, PROJECTILE_GRAVITY);
                    if (projectile != null) {
                        cooldown.setCooldown(info.getCooldown());
                        AbilityUtils.consumeEnderPearl(this, player);
                        EnderPearlAbilities.getInstance().emitEvent(
                                EventListener.class,
                                new AbilityActivateEvent(this),
                                EventListener::onAbilityActivate
                        );
                    }
                }
        ).execute();
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager.hasMetadata("silence")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Entity shooter = (Entity) event.getEntity().getShooter();
        if (shooter != null && shooter.hasMetadata("silence")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        Entity entity = event.getEntity();
        if (entity.hasMetadata("silence")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTeleport(EntityTeleportEvent event) {
        if (event.getEntityType() == EntityType.ENDERMAN) {
            Entity entity = event.getEntity();
            if (entity.hasMetadata("silence")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();

        if (!(shooter instanceof Player player)) return;
        if (!AbilityUtils.verifyAbilityCouple(this, projectile)) return;
        if (!player.getName().equals(ownerName)) return;

        if (!(projectile instanceof Snowball)) return;

        event.setCancelled(true);

        projectile.getWorld().spawnParticle(Particle.SMOKE_NORMAL, projectile.getLocation(), 2, 0.1, 0.1, 0.1, 0.02);

        Location hitPosition = ProjectileUtils.correctProjectileHitLocation(projectile).add(0, 1, 0);

        new AbilityRunnable() {
            @Override
            protected void start() {
                projectile.getWorld().playSound(projectile.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 2);
            }

            @Override
            protected void tick() {
                projectile.getWorld().spawnParticle(Particle.FLAME, hitPosition, 10, ORB_RADIUS / 2, ORB_RADIUS / 2, ORB_RADIUS / 2, 0);
                for (Entity entity : projectile.getWorld().getNearbyEntities(hitPosition, ORB_RADIUS, ORB_RADIUS, ORB_RADIUS)) {
                    if (entity instanceof LivingEntity livingEntity) {
                        if (livingEntity instanceof Player victimPlayer) {
                            if (AbilityLockManager.getInstance().isAbilityLocked(victimPlayer)) return;
                            victimPlayer.damage(4, player);
                            victimPlayer.getWorld().playSound(victimPlayer.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.3f, 2);
                            new AbilityRunnable() {
                                BossBar bossBar;

                                @Override
                                protected void start() {
                                    bossBar = Bukkit.createBossBar(ChatColor.RED + info.getName(), BarColor.RED, BarStyle.SOLID);
                                    bossBar.addPlayer(victimPlayer);
                                    AbilityLockManager.getInstance().lockAbility(victimPlayer);
                                }

                                @Override
                                protected void tick() {
                                    bossBar.setProgress(count / 15.0);
                                    victimPlayer.getWorld().spawnParticle(Particle.FLAME, victimPlayer.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);
                                }

                                @Override
                                protected void end() {
                                    bossBar.removeAll();
                                    AbilityLockManager.getInstance().unlockAbility(victimPlayer);
                                }
                            }.runTaskRepeated(executor, 0, 20, 15);
                        } else {
                            if (livingEntity.hasMetadata("silence")) return;
                            livingEntity.damage(4, player);
                            livingEntity.getWorld().playSound(livingEntity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.3f, 2);
                            new AbilityRunnable() {

                                @Override
                                protected void start() {
                                    livingEntity.setMetadata("silence", new FixedMetadataValue(executor.getPlugin(), true));
                                }

                                @Override
                                protected void tick() {
                                    livingEntity.getWorld().spawnParticle(Particle.FLAME, livingEntity.getEyeLocation(), 20, 0.5, 0.5, 0.5, 0);
                                }

                                @Override
                                protected void end() {
                                    livingEntity.removeMetadata("silence", executor.getPlugin());
                                }
                            }.runTaskRepeated(executor, 0, 20, 15);
                        }
                    }
                }
            }

            @Override
            protected void end() {
                super.end();
            }
        }.runTaskRepeated(this, 0, 1, info.getDuration());

        projectile.remove();
    }
}
