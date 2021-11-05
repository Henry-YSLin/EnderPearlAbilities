package io.github.henry_yslin.enderpearlabilities.utils;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public class MathUtils {

    /**
     * Limit the maximum magnitude of a vector.
     *
     * @param vector    The vector to clamp.
     * @param magnitude The maximum allowed magnitude.
     * @return The same vector with {@code magnitude} as the maximum magnitude.
     */
    public static Vector clamp(Vector vector, double magnitude) {
        magnitude = Math.min(magnitude, vector.length());
        return vector.normalize().multiply(magnitude);
    }

    /**
     * Replace {@code vector} with {@code replacement} if {@code vector} is infinite or NaN.
     *
     * @param vector      The original vector.
     * @param replacement The replacement.
     * @return Either {@code vector} or {@code replacement}.
     */
    public static Vector replaceInfinite(Vector vector, Vector replacement) {
        if (Double.isNaN(vector.length())) return replacement;
        if (Double.isInfinite(vector.length())) return replacement;
        return vector;
    }

    public static boolean almostEqual(double val1, double val2) {
        return almostEqual(val1, val2, 0.000001);
    }

    public static boolean almostEqual(double val1, double val2, double epsilon) {
        return Math.abs(val1 - val2) < epsilon;
    }

    /**
     * Check if a given Location is within a cube described by its center and x, y, z half-lengths
     *
     * @param center The center of the cube.
     * @param x      Half-length of the cube along the x-axis.
     * @param y      Half-length of the cube along the y-axis.
     * @param z      Half-length of the cube along the z-axis.
     * @param loc    The location to check.
     * @return True if the location is within the cube.
     */
    public static boolean isInCube(Location center, double x, double y, double z, Location loc) {
        return Math.abs(loc.getX() - center.getX()) <= x && Math.abs(loc.getY() - center.getY()) <= y && Math.abs(loc.getZ() - center.getZ()) <= z;
    }
}
