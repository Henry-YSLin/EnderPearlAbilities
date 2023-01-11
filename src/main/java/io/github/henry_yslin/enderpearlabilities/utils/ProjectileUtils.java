package io.github.henry_yslin.enderpearlabilities.utils;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ProjectileUtils {

    /**
     * Improve the accuracy of projectile hit location by ray tracing.
     *
     * @param projectile The projectile to compute hit location for. The hit event should have already fired for this projectile.
     * @return The accurate hit location of the projectile, ray traced from its current velocity.
     */
    public static Location correctProjectileHitLocation(Projectile projectile) {
        RayTraceResult result = projectile.getWorld().rayTrace(projectile.getLocation(), projectile.getVelocity(), projectile.getVelocity().length(), FluidCollisionMode.NEVER, true, 0.1, entity -> !entity.equals(projectile));
        if (result == null) {
            return projectile.getLocation();
        }

        return result.getHitPosition().toLocation(projectile.getWorld());
    }

    /*
     * Simulate the movement of an entity with the given parameters.
     * The entity is assumed to be moving with no external force applied and no voluntary direction changes.
     * <p>
     * The location and velocity of the entity are updated in place.
     */
    public static void simulate1TickMovement(Entity entity, Location location, Vector velocity, double gravity, double drag) {
        if (entity.isOnGround()) {
            location.add(velocity.clone().setY(0));
        } else if (EntityUtils.isFlying(entity)) {
            location.add(velocity);
        } else {
            boolean onGround = false;
            if (velocity.getY() <= 0) {
                RayTraceResult rayTraceResult = entity.getWorld().rayTraceBlocks(new Location(location.getWorld(), location.getX(), location.getY() + 0.01, location.getZ()), new Vector(0, -1, 0), 0.02 + velocity.getY(), FluidCollisionMode.ALWAYS, true);
                if (rayTraceResult != null && rayTraceResult.getHitBlock() != null)
                    onGround = true;
            }
            if (onGround)
                velocity.multiply(0);
            location.add(velocity);
            if (EntityUtils.hasDelayedDrag(entity)) {
                velocity.add(new Vector(0, -gravity, 0));
                velocity.multiply(1 - drag);
            } else {
                velocity.multiply(1 - drag);
                velocity.add(new Vector(0, -gravity, 0));
            }
        }
    }

    /**
     * Compute the velocity of a projectile to hit a moving target entity.
     *
     * @param projectile  The projectile to compute velocity for.
     * @param target      The target entity to hit.
     * @param maxVelocity The maximum allowed velocity of the projectile.
     * @param maxAirTime  The maximum allowed air time of the projectile.
     * @return The velocity of the projectile to hit the target.
     */
    public static Vector computeProjectileVelocity(Entity projectile, LivingEntity target, double maxVelocity, int maxAirTime) {
        return computeProjectileVelocity(
                projectile.getLocation(),
                EntityUtils.getGravity(projectile),
                EntityUtils.getDrag(projectile),
                target,
                EntityUtils.getGravity(target),
                EntityUtils.getDrag(target),
                target.getEyeHeight(),
                target.getLocation(),
                target.getVelocity(),
                maxVelocity,
                maxAirTime
        );
    }

    /**
     * Compute the velocity of a projectile to hit a stationary target location.
     *
     * @param projectile  The projectile to compute velocity for.
     * @param target      The target location to hit.
     * @param maxVelocity The maximum allowed velocity of the projectile.
     * @param maxAirTime  The maximum allowed air time of the projectile.
     * @return The velocity of the projectile to hit the target.
     */
    public static Vector computeProjectileVelocity(Entity projectile, Location target, double maxVelocity, int maxAirTime) {
        return computeProjectileVelocity(
                projectile.getLocation(),
                EntityUtils.getGravity(projectile),
                EntityUtils.getDrag(projectile),
                null,
                0,
                0,
                0,
                target,
                new Vector(0, 0, 0),
                maxVelocity,
                maxAirTime
        );
    }

    /**
     * Compute the velocity of a projectile to hit a stationary target location.
     *
     * @param projectileLocation The location of the projectile.
     * @param projectileGravity  The gravity of the projectile.
     * @param projectileDrag     The drag of the projectile.
     * @param target             The target location to hit.
     * @param maxVelocity        The maximum allowed velocity of the projectile.
     * @param maxAirTime         The maximum allowed air time of the projectile.
     * @return The velocity of the projectile to hit the target.
     */
    public static Vector computeProjectileVelocity(
            final Location projectileLocation,
            final double projectileGravity,
            final double projectileDrag,
            Location target,
            double maxVelocity,
            int maxAirTime
    ) {
        return computeProjectileVelocity(
                projectileLocation,
                projectileGravity,
                projectileDrag,
                null,
                0,
                0,
                0,
                target,
                new Vector(0, 0, 0),
                maxVelocity,
                maxAirTime
        );
    }

    public static Vector computeProjectileVelocity(
            final Location projectileLocation,
            final double projectileGravity,
            final double projectileDrag,
            @Nullable final LivingEntity target,
            final double targetGravity,
            final double targetDrag,
            final double targetEyeHeight,
            final Location targetLocation,
            final Vector targetVelocity,
            final double maxVelocity,
            final int maxAirTime
    ) {
        double cumulativeGravity = 0;

        Location computedTargetLoc = targetLocation.clone();
        Vector computedTargetVel = targetVelocity.clone();

        for (int i = 1; i <= maxAirTime; i++) {
            if (target != null)
                simulate1TickMovement(target, computedTargetLoc, computedTargetVel, targetGravity, targetDrag);

            double dragCoefficient = (1 - Math.pow(1 - projectileDrag, i)) / (projectileDrag);

            Location loc = projectileLocation.clone();
            loc.setY(computedTargetLoc.getY());
            double horizontalDistance = computedTargetLoc.distance(loc);
            double horizontalVelocity = horizontalDistance / dragCoefficient;

            double verticalDistance = computedTargetLoc.getY() + targetEyeHeight - projectileLocation.getY();
            double verticalVelocity = (verticalDistance + cumulativeGravity) / dragCoefficient;

            cumulativeGravity += projectileGravity * dragCoefficient;

            if (horizontalVelocity * horizontalVelocity + verticalVelocity * verticalVelocity > maxVelocity * maxVelocity)
                continue;

            return computedTargetLoc.toVector().subtract(projectileLocation.toVector().setY(computedTargetLoc.getY())).normalize().multiply(horizontalVelocity).add(new Vector(0, verticalVelocity, 0));
        }
        return targetLocation.toVector().subtract(projectileLocation.toVector()).normalize().multiply(maxVelocity);
    }
}
