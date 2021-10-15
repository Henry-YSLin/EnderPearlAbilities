package io.github.henryyslin.enderpearlabilities.abilities.seer;

import io.github.henryyslin.enderpearlabilities.utils.AbilityRunnable;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class CylindricalParticleEffect extends AbilityRunnable {
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

    public CylindricalParticleEffect(Particle particle, Location origin, Vector direction, double range, double radius, double angleDelta, int particleCount) {
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
    }

    @Override
    protected void start() {
        Vector temp = forward.clone().add(new Vector(0, 1, 0));
        if (temp.getX() == 0 && temp.getZ() == 0)
            temp.add(new Vector(1, 0, 0));
        lastParticle = forward.getCrossProduct(temp).normalize().multiply(radius);
        forward.multiply(range / particleCount);
        duration = count + 1;
        particlePerTick = (int) (particleCount / duration);
    }

    @Override
    protected void tick() {
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
