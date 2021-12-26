package io.github.henry_yslin.enderpearlabilities.abilities.horizontactical;

import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class GravityLiftRunnable extends AbilityRunnable {

    final LivingEntity entity;
    int preempt = 5;

    public GravityLiftRunnable(LivingEntity entity) {
        this.entity = entity;
    }

    private static boolean shouldActivate(LivingEntity entity) {
        if (entity.getVelocity().getY() >= 0) return false;
        if (entity.getFallDistance() < 3) return false;

        RayTraceResult result = entity.getWorld().rayTraceBlocks(entity.getLocation(), new Vector(0, -1, 0), Math.max(2, -entity.getVelocity().getY() + 0.5), FluidCollisionMode.NEVER, true);
        if (result == null) return false;
        return result.getHitBlock() != null;
    }

    @Override
    protected void start() {
        entity.setMetadata("gravity-lift", new FixedMetadataValue(executor.getPlugin(), true));
    }

    @Override
    public void tick() {
        if (preempt-- > 0) return;
        entity.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, entity.getLocation(), 1, 0, 0, 0, 0.02);
        if (!entity.isValid() || entity.isInWater() || entity.isOnGround()) {
            cancel();
            return;
        }
        if (shouldActivate(entity)) {
            entity.setVelocity(entity.getVelocity().setY(entity.getVelocity().getY() / 5));
            entity.setFallDistance(entity.getFallDistance() - 30);
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR, 0.5f, 0);
            entity.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, entity.getLocation(), 10, 0.1, 0.1, 0.1, 0.02);
            cancel();
        }
    }

    @Override
    protected void end() {
        entity.removeMetadata("gravity-lift", executor.getPlugin());
    }
}
