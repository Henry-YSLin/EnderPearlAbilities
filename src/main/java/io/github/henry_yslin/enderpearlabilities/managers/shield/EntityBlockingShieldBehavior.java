package io.github.henry_yslin.enderpearlabilities.managers.shield;

import io.github.henry_yslin.enderpearlabilities.utils.WorldUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;

public class EntityBlockingShieldBehavior extends ShieldBehavior {
    private static EntityBlockingShieldBehavior instance;

    public static ShieldBehavior getInstance() {
        if (instance == null) {
            instance = new EntityBlockingShieldBehavior();
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
    public void entityWillHit(Shield shield, Entity entity, Location hitPosition, boolean backwardHit) {
        if (entity instanceof LivingEntity) return;
        Vector normal = shield.getNormal().normalize();
        if (backwardHit)
            entity.teleport(hitPosition.clone().add(normal.clone().multiply(0.25)));
        else
            entity.teleport(hitPosition.clone().add(normal.clone().multiply(-0.25)));
        if (entity instanceof Projectile) {
            if (entity.hasGravity()) {
                double magnitude = entity.getVelocity().dot(normal);
                entity.setVelocity(entity.getVelocity().add(normal.multiply(magnitude * -1.2)));
            } else {
                entity.remove();
            }
        } else {
            double magnitude = entity.getVelocity().dot(normal);
            entity.setVelocity(entity.getVelocity().add(normal.multiply(magnitude * -1)));
        }
        shield.getWorld().spawnParticle(Particle.DRAGON_BREATH, hitPosition, 1, 0, 0, 0, 0);
    }
}
