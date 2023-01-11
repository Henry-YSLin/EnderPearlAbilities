package io.github.henry_yslin.enderpearlabilities.abilities.fuseultimate;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.*;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.utils.*;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class FuseUltimateAbility extends Ability<FuseUltimateAbilityInfo> {

    static final int PROJECTILE_LIFETIME = 200;
    static final double PROJECTILE_SPEED = 10;
    static final boolean PROJECTILE_GRAVITY = true;
    static final double MAX_RANGE = 150;
    static final double EXPLOSION_HEIGHT = 15;
    static final double VERTICAL_SCAN_RANGE = 5;
    static final double FIRE_RADIUS = 3;
    static final int PELLET_COUNT = 14;
    static final double PELLET_SPEED = 0.5;

    public FuseUltimateAbility(Plugin plugin, FuseUltimateAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean blockShoot = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    final List<FireRing> fireRings = Collections.synchronizedList(new ArrayList<>());
    BombPredictionRunnable bombPredictionRunnable;
    AbilityRunnable fireRingRunnable;

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

    @Override
    public void onDisable() {
        super.onDisable();
        synchronized (fireRings) {
            for (FireRing fireRing : fireRings) {
                for (FirePellet firePellet : fireRing.pellets) {
                    firePellet.pellet.remove();
                }
            }
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


        if (fireRingRunnable != null && !fireRingRunnable.isCancelled())
            fireRingRunnable.cancel();
        (fireRingRunnable = new AbilityRunnable() {
            @Override
            protected void start() {
                super.start();
            }

            @Override
            protected void tick() {
                super.tick();
                abilityActive.set(fireRings.size() > 0);
                if (fireRings.size() == 0) {
                    return;
                }
                synchronized (fireRings) {
                    for (int i = fireRings.size() - 1; i >= 0; i--) {
                        FireRing fireRing = fireRings.get(i);

                        for (FirePellet pellet : fireRing.pellets) {
                            if (!pellet.isExpired() && pellet.fireLocation != null) {
                                pellet.pellet.getWorld().spawnParticle(Particle.FLAME, pellet.fireLocation, 10, FIRE_RADIUS / 2, FIRE_RADIUS / 2, FIRE_RADIUS / 2, 0.02, null, true);
                                pellet.pellet.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, pellet.fireLocation, 5, FIRE_RADIUS / 2, FIRE_RADIUS / 2, FIRE_RADIUS / 2, 0, null, true);
                                if (Math.random() < 0.2f)
                                    pellet.pellet.getWorld().playSound(pellet.fireLocation, Sound.ENTITY_BLAZE_SHOOT, 0.2f, 0.1f);
                                pellet.pellet.getWorld().getNearbyEntities(pellet.fireLocation, FIRE_RADIUS, FIRE_RADIUS, FIRE_RADIUS).forEach(entity -> {
                                    if (entity instanceof LivingEntity livingEntity) {
                                        livingEntity.addPotionEffect(PotionEffectType.WITHER.createEffect(5 * 20, 2));
                                        livingEntity.addPotionEffect(PotionEffectType.SLOW.createEffect(5 * 20, 3));
                                    }
                                });
                                pellet.lifetime -= 5;
                            }
                        }

                        if (fireRing.cachedBoundingBox != null) {
                            fireRing.pellets.get(0).pellet.getWorld().getNearbyEntities(fireRing.cachedBoundingBox).forEach(entity -> {
                                if (entity instanceof LivingEntity livingEntity) {
                                    if (MathUtils.isInsidePolygon(livingEntity.getLocation().getX(), livingEntity.getLocation().getZ(), fireRing.polygonX, fireRing.polygonY)) {
                                        if (livingEntity instanceof Player p && !livingEntity.hasPotionEffect(PotionEffectType.GLOWING))
                                            p.sendTitle(" ", ChatColor.RED + "Mortar flare detected", 5, 30, 30);
                                        livingEntity.addPotionEffect(PotionEffectType.GLOWING.createEffect(7, 0));
                                    }
                                }
                            });
                        }

                        if (fireRing.isExpired()) {
                            fireRings.remove(i);
                        }
                    }
                }
            }

            @Override
            protected void end() {
                super.end();
            }
        }).runTaskTimer(this, 0, 5);
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

        Optional<Object> ref = EntityUtils.getMetadata(projectile, "pellet");

        projectile.getWorld().spawnParticle(Particle.SMOKE_NORMAL, projectile.getLocation(), 2, 0.1, 0.1, 0.1, 0.02);

        if (ref.isEmpty()) {
            explode(projectile);
        } else {
            Location hitPosition = ProjectileUtils.correctProjectileHitLocation(projectile);
            FirePellet pellet = (FirePellet) ref.get();
            if (event.getHitBlockFace() == BlockFace.UP) {
                pellet.createFire(hitPosition, info.getDuration());
                projectile.getWorld().playSound(hitPosition, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.3f, 0.1f);
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
                    entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.getCodeName(), ownerName)));
                    entity.setVelocity(newVelocity);
                });
                pellet.pellet = snowball;
                snowball.setMetadata("pellet", new FixedMetadataValue(plugin, ref.get()));
            }
        }
        projectile.remove();
    }

    private void explode(Projectile projectile) {
        projectile.remove();
        blockShoot.set(false);

        final Location finalLocation = projectile.getLocation();

        World world = projectile.getWorld();

        FireRing fireRing = new FireRing();
        List<FirePellet> pellets = new ArrayList<>();

        Location horizontal = projectile.getLocation();
        for (int i = 0; i <= PELLET_COUNT; i++) {
            horizontal.setPitch(0);
            horizontal.setYaw(i * 360f / PELLET_COUNT);
            Snowball snowball = projectile.getWorld().spawn(finalLocation, Snowball.class, entity -> {
                entity.setShooter(player);
                entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.getCodeName(), ownerName)));
                entity.setVelocity(horizontal.getDirection().multiply(PELLET_SPEED));
            });
            FirePellet pellet = new FirePellet(fireRing, snowball);
            pellets.add(pellet);
            snowball.setMetadata("pellet", new FixedMetadataValue(plugin, pellet));
        }
        fireRing.addPellets(pellets);
        fireRings.add(fireRing);

        world.playSound(finalLocation, Sound.ENTITY_GENERIC_EXPLODE, 1, 0.1f);
        world.spawnParticle(Particle.EXPLOSION_LARGE, finalLocation, 4, 0, 0, 0, 0, null, true);
    }

    static class FirePellet {
        public final FireRing parentRing;

        public Projectile pellet;

        public int lifetime;

        @Nullable
        private Location fireLocation;

        public FirePellet(FireRing parentRing, Projectile pellet) {
            this.parentRing = parentRing;
            this.pellet = pellet;
        }

        public @Nullable Location getFireLocation() {
            return fireLocation;
        }

        public void createFire(@Nullable Location fireLocation, int lifetime) {
            this.fireLocation = fireLocation;
            this.lifetime = lifetime;
            parentRing.invalidateCache();
        }

        public boolean isExpired() {
            if (this.fireLocation == null) {
                return !this.pellet.isValid();
            } else {
                return this.lifetime <= 0;
            }
        }
    }

    static class FireRing {
        public List<FirePellet> pellets = new ArrayList<>();

        private BoundingBox cachedBoundingBox;
        private double[] polygonX;
        private double[] polygonY;

        public void addPellet(FirePellet pellet) {
            pellets.add(pellet);
            invalidateCache();
        }

        public void addPellets(List<FirePellet> pellets) {
            this.pellets.addAll(pellets);
            invalidateCache();
        }

        public void invalidateCache() {
            double minX = Double.POSITIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;
            int activeFire = 0;
            for (FirePellet pellet : pellets) {
                if (pellet.getFireLocation() == null) continue;
                if (pellet.lifetime <= 0) continue;
                activeFire++;
                minX = Math.min(minX, pellet.getFireLocation().getX());
                maxX = Math.max(maxX, pellet.getFireLocation().getX());
                minY = Math.min(minY, pellet.getFireLocation().getY());
                maxY = Math.max(maxY, pellet.getFireLocation().getY());
                minZ = Math.min(minZ, pellet.getFireLocation().getZ());
                maxZ = Math.max(maxZ, pellet.getFireLocation().getZ());
            }
            if (activeFire < 3) {
                cachedBoundingBox = null;
                return;
            }
            cachedBoundingBox = new BoundingBox(minX, minY - VERTICAL_SCAN_RANGE, minZ, maxX, maxY + VERTICAL_SCAN_RANGE, maxZ);

            polygonX = pellets.stream()
                    .filter(pellet -> pellet.getFireLocation() != null && pellet.lifetime > 0)
                    .mapToDouble(pellet -> pellet.getFireLocation().getX())
                    .toArray();
            polygonY = pellets.stream()
                    .filter(pellet -> pellet.getFireLocation() != null && pellet.lifetime > 0)
                    .mapToDouble(pellet -> pellet.getFireLocation().getZ())
                    .toArray();
        }

        public boolean contains(Vector location) {
            if (cachedBoundingBox == null) invalidateCache();
            if (!cachedBoundingBox.contains(location)) return false;
            return MathUtils.isInsidePolygon(location.getX(), location.getZ(), polygonX, polygonY);
        }

        public boolean isExpired() {
            for (FirePellet pellet : pellets) {
                if (!pellet.isExpired()) return false;
            }
            return true;
        }
    }
}
