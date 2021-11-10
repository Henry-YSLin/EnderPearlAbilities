package io.github.henry_yslin.enderpearlabilities.managers.shield;

import io.github.henry_yslin.enderpearlabilities.utils.WorldUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;

public class ProjectileBlockerShieldBehavior extends ShieldBehavior {
    private static ProjectileBlockerShieldBehavior instance;

    public static ShieldBehavior getInstance() {
        if (instance == null) {
            instance = new ProjectileBlockerShieldBehavior();
        }
        return instance;
    }

    @Override
    public int getTickInterval() {
        return 30;
    }

    @Override
    public void tick(Shield shield) {
        WorldUtils.spawnParticleCubeFilled(shield.getBoundingBox().getMin().toLocation(shield.getWorld()), shield.getBoundingBox().getMax().toLocation(shield.getWorld()), Particle.END_ROD, 2, true);
    }

    @Override
    public void projectileWillHit(Shield shield, Projectile projectile, Location hitPosition, boolean backwardHit) {
        projectile.teleport(hitPosition);
        if (projectile.hasGravity()) {
            Vector normal = shield.getNormal().normalize();
            double magnitude = projectile.getVelocity().dot(normal);
            projectile.setVelocity(projectile.getVelocity().add(normal.multiply(magnitude * -1.2)));
        } else {
            projectile.remove();
        }
        shield.getWorld().spawnParticle(Particle.DRAGON_BREATH, hitPosition, 1, 0, 0, 0, 0);
    }
}
