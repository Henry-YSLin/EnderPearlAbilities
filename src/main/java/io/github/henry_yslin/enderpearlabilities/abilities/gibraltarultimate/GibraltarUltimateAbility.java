package io.github.henry_yslin.enderpearlabilities.abilities.gibraltarultimate;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.*;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.WorldUtils;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.concurrent.atomic.AtomicBoolean;

public class GibraltarUltimateAbility extends Ability<GibraltarUltimateAbilityInfo> {

    static final int PROJECTILE_LIFETIME = 100;
    static final double PROJECTILE_SPEED = 2;
    static final boolean PROJECTILE_GRAVITY = true;
    static final double AIRSTRIKE_RADIUS = 12;
    static final int MISSILE_DELAY = 3;

    public GibraltarUltimateAbility(Plugin plugin, GibraltarUltimateAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean blockShoot = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);

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
        return false;
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
    }

    @EventHandler
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (!AbilityUtils.verifyAbilityCouple(this, event.getEntity())) return;
        event.setCancelled(true);
        event.getEntity().remove();
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;

        event.setCancelled(true);

        if (!cooldown.isAbilityUsable()) return;
        if (abilityActive.get()) return;

        AbilityUtils.fireProjectile(this, player, blockShoot, PROJECTILE_LIFETIME, PROJECTILE_SPEED, PROJECTILE_GRAVITY);
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

        AbilityUtils.consumeEnderPearl(this, player);
        EnderPearlAbilities.getInstance().emitEvent(
                EventListener.class,
                new AbilityActivateEvent(this),
                EventListener::onAbilityActivate
        );

        projectile.remove();
        abilityActive.set(true);
        blockShoot.set(false);

        Location finalLocation = projectile.getLocation();

        World world = projectile.getWorld();

        new FunctionChain(
                nextFunction -> new AbilityRunnable() {
                    @Override
                    protected void start() {
                        WorldUtils.spawnParticleCubeOutline(finalLocation.clone().add(-AIRSTRIKE_RADIUS, 0, -AIRSTRIKE_RADIUS), finalLocation.clone().add(AIRSTRIKE_RADIUS, 1, AIRSTRIKE_RADIUS), Particle.SMOKE_LARGE, 3, true);
                    }

                    @Override
                    protected void tick() {
                        Location location = new Location(
                                world,
                                finalLocation.getX() + AIRSTRIKE_RADIUS * (Math.random() - 0.5) * 2,
                                300,
                                finalLocation.getZ() + AIRSTRIKE_RADIUS * (Math.random() - 0.5) * 2
                        );

                        Location landingLocation;
                        RayTraceResult result = world.rayTraceBlocks(location, new Vector(0, -1, 0), 301, FluidCollisionMode.ALWAYS, true);
                        if (result != null)
                            landingLocation = result.getHitPosition().toLocation(world);
                        else
                            landingLocation = location.clone().subtract(0, location.getY(), 0);

                        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, landingLocation, 5, 0.1, 0.5, 0.1, 0, null, true);

                        TNTPrimed tnt = world.spawn(location, TNTPrimed.class, entity -> {
                            entity.setFuseTicks(info.getDuration() + 10);
                            entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.getCodeName(), ownerName)));
                            entity.setVelocity(new Vector(0, -7.5 - Math.random() * 5, 0));
                        });
                        new MissileAI(player, tnt).runTaskRepeated(executor, 0, 1, info.getDuration());
                    }

                    @Override
                    protected void end() {
                        abilityActive.set(false);
                        cooldown.setCooldown(info.getCooldown());
                    }
                }.runTaskRepeated(this, 0, MISSILE_DELAY, info.getDuration() / MISSILE_DELAY)
        ).execute();
    }
}
