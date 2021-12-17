package io.github.henry_yslin.enderpearlabilities.abilities.bangaloretactical;

import io.github.henry_yslin.enderpearlabilities.abilities.*;
import io.github.henry_yslin.enderpearlabilities.utils.*;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class BangaloreTacticalAbility extends Ability {

    static final int PROJECTILE_LIFETIME = 100;
    static final int SMOKE_PELLET_LIFETIME = 20;
    static final double PROJECTILE_SPEED = 3;
    static final boolean PROJECTILE_GRAVITY = true;
    static final double SMOKE_RADIUS = 4;

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        super.setConfigDefaults(config);
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 400);
        config.addDefault("cooldown", 1000);
    }

    public BangaloreTacticalAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("bangalore-tactical")
                .name("Smoke Launcher")
                .origin("Apex - Bangalore")
                .description("Fire a high-velocity smoke canister that explodes into a smoke wall on impact.\nPassive ability: Taking fire or damage while sprinting makes you move faster for a brief time.")
                .usage("Right click to fire a smoke canister. The smoke wall will deal small damage and protect players from mob targeting.")
                .activation(ActivationHand.OffHand);

        if (config != null)
            builder
                    .chargeUp(config.getInt("charge-up"))
                    .duration(config.getInt("duration"))
                    .cooldown(config.getInt("cooldown"));

        info = builder.build();

        subListeners.add(new DoubleTimeListener(plugin, this, config));
    }

    @Override
    public AbilityInfo getInfo() {
        return info;
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean blockShoot = new AtomicBoolean(false);
    final List<SmokeSpot> smokeSpots = new ArrayList<>();
    final List<Entity> projectiles = Collections.synchronizedList(new ArrayList<>());
    AbilityRunnable smokeRunnable;

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
        synchronized (projectiles) {
            projectiles.forEach(Entity::remove);
        }
    }

    private void setUpPlayer(Player player) {
        blockShoot.set(false);
        cooldown.setCooldown(info.cooldown);

        if (smokeRunnable != null && !smokeRunnable.isCancelled())
            smokeRunnable.cancel();
        (smokeRunnable = new AbilityRunnable() {
            final List<LivingEntity> rescanTargets = new ArrayList<>();

            @Override
            protected void start() {
                super.start();
            }

            @Override
            protected void tick() {
                super.tick();
                if (smokeSpots.size() == 0) return;
                rescanTargets.clear();
                boolean recalculateLineOfSight = false;
                for (int i = smokeSpots.size() - 1; i >= 0; i--) {
                    SmokeSpot smokeSpot = smokeSpots.get(i);
                    World world = Objects.requireNonNull(smokeSpot.location.getWorld());
                    if (smokeSpot.lifetime == info.duration) {
                        recalculateLineOfSight = true;
                        world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, smokeSpot.location, 50, 2, 2, 2, 0.003, null, true);
                    }
                    if (smokeSpot.lifetime > 200)
                        world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, smokeSpot.location, 5, 2, 2, 2, 0.003, null, true);
                    for (Entity entity : world.getNearbyEntities(smokeSpot.location, SMOKE_RADIUS, SMOKE_RADIUS, SMOKE_RADIUS)) {
                        if (entity instanceof Player p) {
                            if (!p.hasPotionEffect(PotionEffectType.INVISIBILITY))
                                rescanTargets.add(p);
                            p.addPotionEffect(PotionEffectType.INVISIBILITY.createEffect(5, 1));
                        } else if (entity instanceof Mob mob) {
                            if (!mob.hasPotionEffect(PotionEffectType.BLINDNESS))
                                rescanTargets.add(mob);
                            mob.addPotionEffect(PotionEffectType.BLINDNESS.createEffect(5, 1));
                            mob.setTarget(null);
                        }
                    }
                    smokeSpot.lifetime--;
                    if (smokeSpot.lifetime <= 0)
                        smokeSpots.remove(i);
                }
                if (rescanTargets.size() > 0) {
                    for (World world : rescanTargets.stream().map(LivingEntity::getWorld).distinct().toList()) {
                        for (LivingEntity entity : world.getLivingEntities()) {
                            if (entity instanceof Mob mob) {
                                if (rescanTargets.contains(mob.getTarget())) {
                                    mob.setTarget(null);
                                }
                            }
                        }
                    }
                }
                if (recalculateLineOfSight) {
                    for (World world : smokeSpots.stream().map(s -> s.location.getWorld()).distinct().toList()) {
                        for (LivingEntity entity : world.getLivingEntities()) {
                            if (entity instanceof Mob mob) {
                                Entity target = mob.getTarget();
                                if (target != null) {
                                    if (smokeSpots.stream().anyMatch(s -> MathUtils.lineBoxIntersect(s.location, SMOKE_RADIUS, SMOKE_RADIUS, SMOKE_RADIUS, target.getLocation(), mob.getLocation()).isPresent()))
                                        mob.setTarget(null);
                                }
                            }
                        }
                    }
                }
            }

            @Override
            protected void end() {
                super.end();
            }
        }).runTaskTimer(this, 0, 1);
    }

    @EventHandler
    public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        if (event.getEntity() instanceof Mob mob) {
            Entity target = event.getTarget();
            if (target != null) {
                if (smokeSpots.stream().anyMatch(smokeSpot -> MathUtils.isInCube(smokeSpot.location, SMOKE_RADIUS, SMOKE_RADIUS, SMOKE_RADIUS, mob.getLocation()))) {
                    event.setCancelled(true);
                    return;
                } else if (smokeSpots.stream().anyMatch(s -> MathUtils.lineBoxIntersect(s.location, SMOKE_RADIUS, SMOKE_RADIUS, SMOKE_RADIUS, target.getLocation(), mob.getLocation()).isPresent())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        Entity target = event.getTarget();
        if (target != null) {
            if (smokeSpots.stream().anyMatch(smokeSpot -> MathUtils.isInCube(smokeSpot.location, SMOKE_RADIUS, SMOKE_RADIUS, SMOKE_RADIUS, target.getLocation()))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) return;

        event.setCancelled(true);

        if (cooldown.isCoolingDown()) return;
        if (chargingUp.get()) return;

        PlayerUtils.consumeEnderPearl(player);
        new FunctionChain(
                next -> AbilityUtils.chargeUpSequence(this, player, info.chargeUp, chargingUp, next),
                next -> {
                    Projectile projectile = AbilityUtils.fireProjectile(this, player, blockShoot, PROJECTILE_LIFETIME, PROJECTILE_SPEED, PROJECTILE_GRAVITY);
                    if (projectile != null)
                        projectiles.add(projectile);
                    cooldown.setCooldown(info.cooldown);
                }
        ).execute();
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

        Location hitPosition = ProjectileUtils.correctProjectileHitLocation(projectile);

        Optional<Object> ref = EntityUtils.getMetadata(projectile, "smoke");
        if (ref.isEmpty()) {
            for (int i = -2; i <= 2; i++) {
                Vector horizontal = projectile.getVelocity().getCrossProduct(new Vector(0, 1, 0)).normalize().multiply(0.2);
                int finalI = i;
                Snowball snowball = projectile.getWorld().spawn(hitPosition, Snowball.class, entity -> {
                    entity.setShooter(player);
                    entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.codeName, ownerName)));
                    entity.setVelocity(horizontal.clone().multiply(finalI).setY(0.25));
                });
                projectiles.add(snowball);
                snowball.setMetadata("smoke", new FixedMetadataValue(plugin, SMOKE_PELLET_LIFETIME));
            }
            projectile.getWorld().playSound(hitPosition, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1, 1);
        } else {
            int lifetime = (int) ref.get();
            lifetime -= projectile.getTicksLived();
            if (lifetime <= 0) {
                SmokeSpot spot = new SmokeSpot(hitPosition.add(0, player.getEyeHeight(true), 0), info.duration);
                World world = Objects.requireNonNull(spot.location.getWorld());
                smokeSpots.add(spot);
                for (Entity nearbyEntity : world.getNearbyEntities(spot.location, SMOKE_RADIUS, SMOKE_RADIUS, SMOKE_RADIUS, entity -> entity instanceof LivingEntity && entity != player)) {
                    LivingEntity livingEntity = (LivingEntity) nearbyEntity;
                    Vector offset = livingEntity.getEyeLocation().toVector().subtract(spot.location.toVector());
                    RayTraceResult result = world.rayTraceBlocks(spot.location, offset, offset.length(), FluidCollisionMode.NEVER, true);
                    if (result == null)
                        ((LivingEntity) nearbyEntity).damage(2, player);
                }
                projectile.getWorld().playSound(hitPosition, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.5f, 0);
            } else {
                Vector newVelocity;
                if (event.getHitBlockFace() != null) {
                    double hitMagnitude = Math.abs(projectile.getVelocity().dot(event.getHitBlockFace().getDirection()));
                    newVelocity = projectile.getVelocity().add(event.getHitBlockFace().getDirection().multiply(hitMagnitude)).multiply(0.7).add(event.getHitBlockFace().getDirection().multiply(hitMagnitude * 0.8));

                } else {
                    newVelocity = projectile.getVelocity().setX(0).setZ(0);
                }
                Snowball snowball = projectile.getWorld().spawn(hitPosition, Snowball.class, entity -> {
                    entity.setShooter(player);
                    entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.codeName, ownerName)));
                    entity.setVelocity(newVelocity);
                });
                projectiles.add(snowball);
                snowball.setMetadata("smoke", new FixedMetadataValue(plugin, lifetime));
            }
        }
        projectiles.remove(projectile);
        projectile.remove();
    }

    private static class SmokeSpot {
        public Location location;
        public int lifetime;

        public SmokeSpot(Location location, int lifetime) {
            this.location = location;
            this.lifetime = lifetime;
        }
    }
}
