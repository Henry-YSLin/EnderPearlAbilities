package io.github.henry_yslin.enderpearlabilities.abilities.gibraltarultimate;

import io.github.henry_yslin.enderpearlabilities.abilities.*;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.PlayerUtils;
import io.github.henry_yslin.enderpearlabilities.utils.WorldUtils;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
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

public class GibraltarUltimateAbility extends Ability {

    static final int PROJECTILE_LIFETIME = 100;
    static final double PROJECTILE_SPEED = 2;
    static final boolean PROJECTILE_GRAVITY = true;
    static final double AIRSTRIKE_RADIUS = 12;
    static final int MISSILE_DELAY = 3;

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        super.setConfigDefaults(config);
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 120);
        config.addDefault("cooldown", 1500);
    }

    public GibraltarUltimateAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("gibraltar-ultimate")
                .name("Defensive Bombardment")
                .origin("Apex - Gibraltar")
                .description("Call in a concentrated mortar strike on a marked position.")
                .usage("Right click to throw a flare that marks a radius for continuous bombardment.")
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

    final AtomicBoolean blockShoot = new AtomicBoolean(false);
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
        abilityActive.set(false);
        blockShoot.set(false);
        cooldown.startCooldown(info.cooldown);
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

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;
        if (abilityActive.get()) return;

        PlayerUtils.consumeEnderPearl(player);
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
                            entity.setFuseTicks(info.duration + 10);
                            entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.codeName, ownerName)));
                            entity.setVelocity(new Vector(0, -7.5 - Math.random() * 5, 0));
                        });
                        new MissileAI(player, tnt).runTaskRepeated(executor, 0, 1, info.duration);
                    }

                    @Override
                    protected void end() {
                        abilityActive.set(false);
                    }
                }.runTaskRepeated(this, 0, MISSILE_DELAY, info.duration / MISSILE_DELAY)
        ).execute();
    }
}
