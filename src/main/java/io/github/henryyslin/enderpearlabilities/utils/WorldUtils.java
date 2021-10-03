package io.github.henryyslin.enderpearlabilities.utils;

import org.bukkit.World;

public class WorldUtils {
    public static boolean isDaytime(World world) {
        return world.getTime() < 12000;
    }
}
