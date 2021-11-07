package io.github.henry_yslin.enderpearlabilities.abilities.bangaloreultimate;

import io.github.henry_yslin.enderpearlabilities.abilities.*;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.PlayerUtils;
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
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class BangaloreUltimateAbility extends Ability {

    static final int PROJECTILE_LIFETIME = 100;
    static final double PROJECTILE_SPEED = 2;
    static final boolean PROJECTILE_GRAVITY = true;
    static final int MISSILE_ARRAY_SIZE = 6;
    static final double MISSILE_SPACING = 10;
    static final double EXPLOSION_RADIUS = 5;

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        super.setConfigDefaults(config);
        config.addDefault("charge-up", 120);
        config.addDefault("duration", 120);
        config.addDefault("cooldown", 1000);
    }

    public BangaloreUltimateAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("bangalore-ultimate")
                .name("Rolling Thunder")
                .origin("Apex - Bangalore")
                .description("Call in an artillery strike that slowly creeps across the landscape.")
                .usage("Right click to throw a flare. Several rows of missiles will then land sequentially in front of the flare and stick for 6 seconds before exploding, slowing and blinding entities.")
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
        event.getEntity().getWorld().createExplosion(event.getEntity().getLocation(), 4, false, false);
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

        Vector forward = projectile.getVelocity().setY(0).normalize().multiply(MISSILE_SPACING);
        Vector sideways = forward.getCrossProduct(new Vector(0, 1, 0)).normalize().multiply(MISSILE_SPACING);
        Location origin = finalLocation.add(sideways.clone().multiply(-(MISSILE_ARRAY_SIZE - 1) / 2));

        List<Location> missileLocations = new ArrayList<>(MISSILE_ARRAY_SIZE * MISSILE_ARRAY_SIZE);
        for (int i = 0; i < MISSILE_ARRAY_SIZE; i++) {
            Location rowOrigin = origin.clone().add(forward.clone().multiply(i));
            if (i % 2 == 0)
                rowOrigin.add(sideways.clone().multiply(MISSILE_ARRAY_SIZE));
            for (int j = 0; j < MISSILE_ARRAY_SIZE; j++) {
                missileLocations.add(rowOrigin.clone().add(sideways.clone().multiply(j * (i % 2 == 0 ? -1 : 1))));
            }
        }

        World world = projectile.getWorld();

        new FunctionChain(
                nextFunction -> new AbilityRunnable() {
                    @Override
                    protected void start() {
                        for (Location location : missileLocations) {
                            World world = Objects.requireNonNull(location.getWorld());
                            Location landingLocation = world.getHighestBlockAt(location).getLocation().add(0.5, 1.5, 0.5);

                            // todo: should not be force
                            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, landingLocation, 5, 0.1, 0.5, 0.1, 0, null, true);
                        }
                    }

                    @Override
                    protected void tick() {
                        Location location = missileLocations.remove(0);
                        World world = Objects.requireNonNull(location.getWorld());
                        location.setY(300);
                        TNTPrimed tnt = world.spawn(location, TNTPrimed.class, entity -> {
                            entity.setFuseTicks(info.duration);
                            entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.codeName, ownerName)));
                            entity.setVelocity(new Vector(0, -10, 0));
                        });
                        new MissileAI(player, tnt).runTaskRepeated(executor, 0, 1, info.duration);
                    }

                    @Override
                    protected void end() {
                        abilityActive.set(false);
                    }
                }.runTaskRepeated(this, 0, Math.max(1, info.chargeUp / MISSILE_ARRAY_SIZE / MISSILE_ARRAY_SIZE), MISSILE_ARRAY_SIZE * MISSILE_ARRAY_SIZE)
        ).execute();
    }
}
