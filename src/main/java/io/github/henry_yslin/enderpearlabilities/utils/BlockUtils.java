package io.github.henry_yslin.enderpearlabilities.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class BlockUtils {

    /**
     * Get the 6 blocks touching a block at the given location.
     *
     * @param center The target block.
     * @return A list of all 6 blocks surrounding the target block.
     */
    public static List<Block> getTouchingBlocks(Location center) {
        World world = Objects.requireNonNull(center.getWorld());

        int x = center.getBlockX();
        int y = center.getBlockY();
        int z = center.getBlockZ();

        ArrayList<Block> blocks = new ArrayList<>();
        blocks.add(world.getBlockAt(x + 1, y, z));
        blocks.add(world.getBlockAt(x - 1, y, z));
        blocks.add(world.getBlockAt(x, y + 1, z));
        blocks.add(world.getBlockAt(x, y - 1, z));
        blocks.add(world.getBlockAt(x, y, z + 1));
        blocks.add(world.getBlockAt(x, y, z - 1));
        return blocks;
    }

    /**
     * Get a cube of blocks within a given center and radius.
     *
     * @param center The center of the cube.
     * @param radius The radius of the cube. For a radius of {@code n}, the block cube will have a side length of {@code 2n + 1}.
     * @return A list of all blocks in the cube.
     */
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

    /**
     * Get a cube of blocks within a given center and radius, returning blocks that pass the filter.
     *
     * @param center The center of the cube.
     * @param radius The radius of the cube. For a radius of {@code n}, the block cube will have a side length of {@code 2n + 1}.
     * @param filter Filter the blocks in the cube.
     * @return A list of all blocks in the cube that pass the filter.
     */
    public static List<Block> getBlocks(Location center, int radius, Predicate<Block> filter) {
        return getBlocks(center, radius).stream().filter(filter).toList();
    }

    /**
     * Check whether a block is safe for spawning an entity, if the entity is spawned with their feet inside this block.
     *
     * @param block The block to check.
     * @return Whether the block is safe for spawning.
     */
    public static boolean isSafeSpawningBlock(Block block) {
        World world = block.getWorld();

        if (block.getType().isOccluding() || block.getType().isSolid()) return false;
        Material above = world.getBlockAt(block.getLocation().add(0, 1, 0)).getType();
        if (above.isOccluding() || above.isSolid()) return false;
        Material ground = world.getBlockAt(block.getLocation().add(0, -1, 0)).getType();
        if (!ground.isSolid()) return false;
        return !ground.isAir() && ground != Material.WATER && ground != Material.LAVA && ground != Material.FIRE && ground != Material.MAGMA_BLOCK;
    }
}
