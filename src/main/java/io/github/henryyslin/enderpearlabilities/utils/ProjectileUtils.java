package io.github.henryyslin.enderpearlabilities.utils;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Projectile;
import org.bukkit.util.RayTraceResult;

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
}
