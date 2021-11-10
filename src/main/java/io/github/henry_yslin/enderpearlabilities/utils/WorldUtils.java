package io.github.henry_yslin.enderpearlabilities.utils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.Objects;

public class WorldUtils {

    /**
     * Check whether the world is in daytime.
     *
     * @param world The world to check.
     * @return Whether the world is in daytime.
     */
    public static boolean isDaytime(World world) {
        return world.getTime() < 12000;
    }

    /**
     * Display particles in a line.
     *
     * @param start    The start position of the line.
     * @param end      The end position of the line.
     * @param particle The type of particle to display.
     * @param density  The number of particles to display per meter.
     * @param force    Whether to send the particle to players within an extended
     *                 range and encourage their client to render it regardless of
     *                 settings
     */
    public static void spawnParticleLine(Location start, Location end, Particle particle, double density, boolean force) {
        World world = start.getWorld();
        if (world == null) return;

        Vector offset = end.clone().subtract(start).toVector().normalize().multiply(1 / density);
        Location current = start.clone();
        int count = (int) (end.distance(start) * density);
        for (int i = 0; i < count; i++) {
            world.spawnParticle(particle, current, 1, 0, 0, 0, 0, null, force);
            current.add(offset);
        }
    }

    /**
     * Display particles in a cuboid outline.
     *
     * @param corner1  The first corner of the cuboid.
     * @param corner2  The opposite corner of the cuboid.
     * @param particle The type of particle to display.
     * @param density  The number of particles to display per meter.
     * @param force    Whether to send the particle to players within an extended
     *                 range and encourage their client to render it regardless of
     *                 settings
     */
    public static void spawnParticleCubeOutline(Location corner1, Location corner2, Particle particle, double density, boolean force) {
        World world = Objects.requireNonNull(corner1.getWorld());

        double x1 = Math.min(corner1.getX(), corner2.getX());
        double x2 = Math.max(corner1.getX(), corner2.getX());
        for (double x = x1; x < x2; x += 1 / density) {
            world.spawnParticle(particle, new Location(world, x, corner1.getY(), corner1.getZ()), 1, 0, 0, 0, 0, null, force);
            world.spawnParticle(particle, new Location(world, x, corner1.getY(), corner2.getZ()), 1, 0, 0, 0, 0, null, force);
            world.spawnParticle(particle, new Location(world, x, corner2.getY(), corner1.getZ()), 1, 0, 0, 0, 0, null, force);
            world.spawnParticle(particle, new Location(world, x, corner2.getY(), corner2.getZ()), 1, 0, 0, 0, 0, null, force);
        }

        double y1 = Math.min(corner1.getY(), corner2.getY());
        double y2 = Math.max(corner1.getY(), corner2.getY());
        for (double y = y1; y < y2; y += 1 / density) {
            world.spawnParticle(particle, new Location(world, corner1.getX(), y, corner1.getZ()), 1, 0, 0, 0, 0, null, force);
            world.spawnParticle(particle, new Location(world, corner1.getX(), y, corner2.getZ()), 1, 0, 0, 0, 0, null, force);
            world.spawnParticle(particle, new Location(world, corner2.getX(), y, corner1.getZ()), 1, 0, 0, 0, 0, null, force);
            world.spawnParticle(particle, new Location(world, corner2.getX(), y, corner2.getZ()), 1, 0, 0, 0, 0, null, force);
        }

        double z1 = Math.min(corner1.getZ(), corner2.getZ());
        double z2 = Math.max(corner1.getZ(), corner2.getZ());
        for (double z = z1; z < z2; z += 1 / density) {
            world.spawnParticle(particle, new Location(world, corner1.getX(), corner1.getY(), z), 1, 0, 0, 0, 0, null, force);
            world.spawnParticle(particle, new Location(world, corner1.getX(), corner2.getY(), z), 1, 0, 0, 0, 0, null, force);
            world.spawnParticle(particle, new Location(world, corner2.getX(), corner1.getY(), z), 1, 0, 0, 0, 0, null, force);
            world.spawnParticle(particle, new Location(world, corner2.getX(), corner2.getY(), z), 1, 0, 0, 0, 0, null, force);
        }
    }

    public static void spawnParticleCubeFilled(Location corner1, Location corner2, Particle particle, double density, boolean force) {
        World world = Objects.requireNonNull(corner1.getWorld());

        for (double x = corner1.getX(); MathUtils.almostSmaller(x, corner2.getX()); x += 1 / density) {
            for (double y = corner1.getY(); MathUtils.almostSmaller(y, corner2.getY()); y += 1 / density) {
                for (double z = corner1.getZ(); MathUtils.almostSmaller(z, corner2.getZ()); z += 1 / density) {
                    world.spawnParticle(particle, new Location(world, x, y, z), 1, 0, 0, 0, 0, null, force);
                }
            }
        }
    }
}
