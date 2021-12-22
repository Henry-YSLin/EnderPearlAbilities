package io.github.henry_yslin.enderpearlabilities.abilities.seertactical;

import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class SeerTacticalEffect extends AbilityRunnable {
    Vector lastParticle;
    Vector forward;
    World world;
    Location origin;
    double radius;
    double angleDelta;
    int particleCount;
    int particlePerTick;
    long duration;
    double range;
    Particle particle;
    boolean playSound;

    public SeerTacticalEffect(Particle particle, Location origin, Vector direction, double range, double radius, double angleDelta, int particleCount, boolean playSound) {
        this.particle = particle;
        this.origin = origin;
        this.world = origin.getWorld();
        if (this.world == null)
            throw new IllegalArgumentException("The origin location must have a world.");
        this.forward = direction;
        this.radius = radius;
        this.angleDelta = angleDelta;
        this.particleCount = particleCount;
        this.range = range;
        this.playSound = playSound;
    }

    @Override
    protected void start() {
        Vector temp = forward.clone().add(new Vector(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5));
        if (temp.equals(forward)) {
            temp.add(new Vector(1, 1, 1));
        }
        lastParticle = forward.getCrossProduct(temp).normalize().multiply(radius);
        forward.multiply(range / particleCount);
        duration = count + 1;
        particlePerTick = (int) (particleCount / duration);
    }

    @Override
    protected void tick() {
        if (playSound) {
            Location newOrigin = origin.clone().add(forward.clone().multiply((duration - count - 1) * particlePerTick));
            world.playSound(newOrigin, Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 2);
        }
        for (int i = 0; i < particlePerTick; i++) {
            world.spawnParticle(particle, origin.clone().add(forward.clone().multiply((duration - count - 1) * particlePerTick + i)).add(lastParticle), 1, 0, 0, 0, 0, null, true);
            lastParticle.rotateAroundAxis(forward, angleDelta);
        }
    }

    @Override
    protected void end() {
        super.end();
    }
}
