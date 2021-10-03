package io.github.henryyslin.enderpearlabilities.utils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

public class WorldUtils {
    public static boolean isDaytime(World world) {
        return world.getTime() < 12000;
    }

    public static void spawnParticleRect(Location corner1, Location corner2, Particle particle, double density) {
        World world = corner1.getWorld();
        if (world == null) return;

        double x1 = Math.min(corner1.getX(), corner2.getX());
        double x2 = Math.max(corner1.getX(), corner2.getX());
        for (double x = x1; x < x2; x += 1 / density) {
            world.spawnParticle(particle, new Location(world, x, corner1.getY(), corner1.getZ()), 1, 0, 0, 0, 0);
            world.spawnParticle(particle, new Location(world, x, corner1.getY(), corner2.getZ()), 1, 0, 0, 0, 0);
            world.spawnParticle(particle, new Location(world, x, corner2.getY(), corner1.getZ()), 1, 0, 0, 0, 0);
            world.spawnParticle(particle, new Location(world, x, corner2.getY(), corner2.getZ()), 1, 0, 0, 0, 0);
        }

        double y1 = Math.min(corner1.getY(), corner2.getY());
        double y2 = Math.max(corner1.getY(), corner2.getY());
        for (double y = y1; y < y2; y += 1 / density) {
            world.spawnParticle(particle, new Location(world, corner1.getX(), y, corner1.getZ()), 1, 0, 0, 0, 0);
            world.spawnParticle(particle, new Location(world, corner1.getX(), y, corner2.getZ()), 1, 0, 0, 0, 0);
            world.spawnParticle(particle, new Location(world, corner2.getX(), y, corner1.getZ()), 1, 0, 0, 0, 0);
            world.spawnParticle(particle, new Location(world, corner2.getX(), y, corner2.getZ()), 1, 0, 0, 0, 0);
        }

        double z1 = Math.min(corner1.getZ(), corner2.getZ());
        double z2 = Math.max(corner1.getZ(), corner2.getZ());
        for (double z = z1; z < z2; z += 1 / density) {
            world.spawnParticle(particle, new Location(world, corner1.getX(), corner1.getY(), z), 1, 0, 0, 0, 0);
            world.spawnParticle(particle, new Location(world, corner1.getX(), corner2.getY(), z), 1, 0, 0, 0, 0);
            world.spawnParticle(particle, new Location(world, corner2.getX(), corner1.getY(), z), 1, 0, 0, 0, 0);
            world.spawnParticle(particle, new Location(world, corner2.getX(), corner2.getY(), z), 1, 0, 0, 0, 0);
        }
    }
}
