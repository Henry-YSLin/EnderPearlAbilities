package io.github.henry_yslin.enderpearlabilities.abilities.fuseultimate;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityCooldown;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.SingleUseCooldown;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.ProjectileUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

public class FuseUltimateAbility extends Ability<FuseUltimateAbilityInfo> {

    static final int PROJECTILE_LIFETIME = 200;
    static final double PROJECTILE_SPEED = 10;
    static final boolean PROJECTILE_GRAVITY = true;
    static final double MAX_RANGE = 150;
    static final double EXPLOSION_HEIGHT = 20;

    public FuseUltimateAbility(Plugin plugin, FuseUltimateAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean blockShoot = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    BombPredictionRunnable bombPredictionRunnable;

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
        abilityActive.set(false);
        blockShoot.set(false);
        cooldown.setCooldown(info.getCooldown());

        if (bombPredictionRunnable != null && !bombPredictionRunnable.isCancelled())
            bombPredictionRunnable.cancel();
        bombPredictionRunnable = new BombPredictionRunnable(this, player);
        bombPredictionRunnable.runTaskTimer(this, 0, 3);
    }

    @Nullable
    Location getExplosionLocation(Player player) {
        RayTraceResult result = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), MAX_RANGE, FluidCollisionMode.NEVER, true);
        if (result == null) {
            return null;
        }
        return result.getHitPosition().toLocation(player.getWorld()).add(0, EXPLOSION_HEIGHT, 0);
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;

        event.setCancelled(true);

        if (!cooldown.isAbilityUsable()) return;
        if (chargingUp.get()) return;
        if (abilityActive.get()) return;

        new FunctionChain(
                next -> AbilityUtils.chargeUpSequence(this, player, info.getChargeUp(), chargingUp, next),
                next -> {
                    Location target = getExplosionLocation(player);
                    if (target == null) {
                        player.sendTitle(" ", ChatColor.LIGHT_PURPLE + "Invalid target", 5, 20, 10);
                        return;
                    }
                    Projectile projectile = AbilityUtils.fireProjectile(this, player, blockShoot, PROJECTILE_LIFETIME, PROJECTILE_SPEED, PROJECTILE_GRAVITY);
                    if (projectile != null) {
                        Vector velocity = ProjectileUtils.computeProjectileVelocity(projectile, target, PROJECTILE_SPEED, PROJECTILE_LIFETIME);
                        projectile.setVelocity(velocity);

                        AbilityUtils.consumeEnderPearl(this, player);
                        EnderPearlAbilities.getInstance().emitEvent(
                                EventListener.class,
                                new AbilityActivateEvent(this),
                                EventListener::onAbilityActivate
                        );

                        new AbilityRunnable() {
                            @Override
                            protected void start() {
                            }

                            @Override
                            protected void tick() {
                                if (!projectile.isValid()) {
                                    cancel();
                                    return;
                                }
                                if (projectile.getLocation().distanceSquared(target) < projectile.getVelocity().lengthSquared() + 1) {
                                    projectile.teleport(target);
                                    cancel();
                                    explode(projectile);
                                    return;
                                }
                                projectile.getWorld().spawnParticle(Particle.SMOKE_LARGE, projectile.getLocation(), 2, 0.1, 0.1, 0.1, 0.02, null, true);
                            }

                            @Override
                            protected void end() {
                            }
                        }.runTaskRepeated(this, 0, 1, PROJECTILE_LIFETIME);
                    }
                }
        ).execute();
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();

        if (!(shooter instanceof Player shooterPlayer)) return;
        if (!AbilityUtils.verifyAbilityCouple(this, projectile)) return;
        if (!shooterPlayer.getName().equals(ownerName)) return;

        if (!(projectile instanceof Snowball)) return;

        event.setCancelled(true);

        explode(projectile);
    }

    private void explode(Projectile projectile) {
        projectile.remove();
        //abilityActive.set(true);
        blockShoot.set(false);

        Location finalLocation = projectile.getLocation();

        World world = projectile.getWorld();

        world.playSound(finalLocation, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
        world.spawnParticle(Particle.EXPLOSION_LARGE, finalLocation, 4, 0, 0, 0, 0, null, true);
    }
}
