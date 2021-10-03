package io.github.henryyslin.enderpearlabilities.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class BlockUtils {
    public static List<Block> getBlocks(Location center, int radius) {
        ArrayList<Block> blocks = new ArrayList<>();
        for (double x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (double y = center.getY() - radius; y <= center.getY() + radius; y++) {
                for (double z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    Location loc = new Location(center.getWorld(), x, y, z);
                    blocks.add(loc.getBlock());
                }
            }
        }
        return blocks;
    }

    public static List<Block> getSafeSpawningBlocks(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) return new ArrayList<>();
        return getBlocks(center, radius).stream().filter(block -> {
            if (block.getType().isOccluding() || block.getType().isSolid()) return false;
            Material above = world.getBlockAt(block.getLocation().add(0, 1, 0)).getType();
            if (above.isOccluding() || above.isSolid()) return false;
            Material ground = world.getBlockAt(block.getLocation().add(0, -1, 0)).getType();
            if (!ground.isSolid()) return false;
            return !ground.isAir() && ground != Material.WATER && ground != Material.LAVA && ground != Material.FIRE && ground != Material.MAGMA_BLOCK;
        }).toList();
    }
}
