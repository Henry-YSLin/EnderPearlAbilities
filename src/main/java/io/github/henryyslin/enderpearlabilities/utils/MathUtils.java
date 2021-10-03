package io.github.henryyslin.enderpearlabilities.utils;

import org.bukkit.util.Vector;

public class MathUtils {
    public static Vector clamp(Vector vector, double magnitude) {
        magnitude = Math.min(magnitude, vector.length());
        return vector.normalize().multiply(magnitude);
    }

    public static Vector replaceInfinite(Vector vector, Vector replacement) {
        if (Double.isNaN(vector.length())) return replacement;
        if (Double.isInfinite(vector.length())) return replacement;
        return vector;
    }
}
