package io.github.henry_yslin.enderpearlabilities.utils;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.Optional;

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

    public static void copyVector(Vector from, Vector to) {
        to.setX(from.getX());
        to.setY(from.getY());
        to.setZ(from.getZ());
    }

    public static Optional<Vector> lineRectangleIntersect(Vector lineStart, Vector lineEnd, Vector corner1, Vector corner2, Vector normal) {
        return linePlaneIntersect(lineStart, lineEnd, normal, corner1.clone().add(corner2).multiply(0.5)).map(intersect -> {
            if (intersect.getX() >= Math.min(corner1.getX(), corner2.getX()) && intersect.getX() <= Math.max(corner1.getX(), corner2.getX()) &&
                    intersect.getY() >= Math.min(corner1.getY(), corner2.getY()) && intersect.getY() <= Math.max(corner1.getY(), corner2.getY()) &&
                    intersect.getZ() >= Math.min(corner1.getZ(), corner2.getZ()) && intersect.getZ() <= Math.max(corner1.getZ(), corner2.getZ())
            ) {
                return intersect;
            }
            return null;
        });
    }

    /**
     * Check if a line intersect with a plane.
     *
     * @param p1          The start point of the line.
     * @param p2          The end point of the line.
     * @param planeNormal The normal of the plane.
     * @param planePoint  A point on the plane.
     * @return The intersection point if the line intersects the plane, otherwise Optional.empty().
     */
    public static Optional<Vector> linePlaneIntersect(Vector p1, Vector p2, Vector planeNormal, Vector planePoint) {
        Vector ray = p2.clone().subtract(p1);
        double d = planeNormal.dot(planePoint);
        if (almostEqual(planeNormal.dot(ray), 0))
            return Optional.empty();
        double t = (d - planeNormal.dot(p1)) / planeNormal.dot(ray);
        if (t < 0 || t > 1)
            return Optional.empty();
        return Optional.of(p1.clone().add(ray.multiply(t)));
    }

    // check if a line intersect with a cube
    // https://stackoverflow.com/questions/4578967/check-if-a-line-intersects-with-a-cube
    public static Optional<Vector> lineBoxIntersect(Location center, double x, double y, double z, Location start, Location end) {
        return lineBoxIntersect(center.clone().subtract(x, y, z).toVector(), center.clone().add(x, y, z).toVector(), start.toVector(), end.toVector());
    }

    /**
     * Check if a line intersect with a cube.
     *
     * @param box1  First corner of cube, the coordinates of this corner must all be smaller than {@code box2}.
     * @param box2  Second corner of cube, the coordinates of this corner must all be larger than {@code box1}.
     * @param line1 First point of line.
     * @param line2 Second point of line.
     * @return The intersection point if the line intersects the cube, otherwise Optional.empty().
     */
    // https://stackoverflow.com/questions/3235385/given-a-bounding-box-and-a-line-two-points-determine-if-the-line-intersects-t
    public static Optional<Vector> lineBoxIntersect(Vector box1, Vector box2, Vector line1, Vector line2) {
        Vector hit = new Vector();
        if (line2.getX() < box1.getX() && line1.getX() < box1.getX()) return Optional.empty();
        if (line2.getX() > box2.getX() && line1.getX() > box2.getX()) return Optional.empty();
        if (line2.getY() < box1.getY() && line1.getY() < box1.getY()) return Optional.empty();
        if (line2.getY() > box2.getY() && line1.getY() > box2.getY()) return Optional.empty();
        if (line2.getZ() < box1.getZ() && line1.getZ() < box1.getZ()) return Optional.empty();
        if (line2.getZ() > box2.getZ() && line1.getZ() > box2.getZ()) return Optional.empty();
        if (line1.getX() > box1.getX() && line1.getX() < box2.getX() &&
                line1.getY() > box1.getY() && line1.getY() < box2.getY() &&
                line1.getZ() > box1.getZ() && line1.getZ() < box2.getZ()) {
            return Optional.of(line1.clone());
        }
        if (getIntersection(line1.getX() - box1.getX(), line2.getX() - box1.getX(), line1, line2, hit) && inBox(hit, box1, box2, 1)
                || (getIntersection(line1.getY() - box1.getY(), line2.getY() - box1.getY(), line1, line2, hit) && inBox(hit, box1, box2, 2))
                || (getIntersection(line1.getZ() - box1.getZ(), line2.getZ() - box1.getZ(), line1, line2, hit) && inBox(hit, box1, box2, 3))
                || (getIntersection(line1.getX() - box2.getX(), line2.getX() - box2.getX(), line1, line2, hit) && inBox(hit, box1, box2, 1))
                || (getIntersection(line1.getY() - box2.getY(), line2.getY() - box2.getY(), line1, line2, hit) && inBox(hit, box1, box2, 2))
                || (getIntersection(line1.getZ() - box2.getZ(), line2.getZ() - box2.getZ(), line1, line2, hit) && inBox(hit, box1, box2, 3))) {
            return Optional.of(hit);
        }
        return Optional.empty();
    }

    private static boolean getIntersection(double fDst1, double fDst2, Vector p1, Vector p2, Vector hit) {
        if ((fDst1 * fDst2) >= 0.0f) return false;
        if (fDst1 == fDst2) return false;
        copyVector(p1.add(p2.subtract(p1).multiply(-fDst1 / (fDst2 - fDst1))), hit);
        return true;
    }

    private static boolean inBox(Vector hit, Vector b1, Vector b2, int axis) {
        if (axis == 1 && hit.getZ() > b1.getZ() && hit.getZ() < b2.getZ() && hit.getY() > b1.getY() && hit.getY() < b2.getY())
            return true;
        if (axis == 2 && hit.getZ() > b1.getZ() && hit.getZ() < b2.getZ() && hit.getX() > b1.getX() && hit.getX() < b2.getX())
            return true;
        if (axis == 3 && hit.getX() > b1.getX() && hit.getX() < b2.getX() && hit.getY() > b1.getY() && hit.getY() < b2.getY())
            return true;
        return false;
    }
}
