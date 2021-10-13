package io.github.henryyslin.enderpearlabilities.utils;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.Optional;

public class PlayerUtils {

    /**
     * Get the entity currently under the given player's crosshair.
     *
     * @param player The player to check.
     * @return The entity under the player's crosshair.
     */
    public static Entity getPlayerTargetEntity(Player player) {
        World world = player.getWorld();
        RayTraceResult result = world.rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 48, entity -> {
            if (entity instanceof Player p) {
                return !p.getName().equals(player.getName());
            }
            return true;
        });
        if (result == null) return null;
        return result.getHitEntity();
    }

    /**
     * Get the living entity currently under the given player's crosshair.
     *
     * @param player The player to check.
     * @return The living entity under the player's crosshair.
     */
    public static LivingEntity getPlayerTargetLivingEntity(Player player) {
        World world = player.getWorld();
        RayTraceResult result = world.rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 48, entity -> {
            if (!(entity instanceof LivingEntity)) return false;
            if (entity instanceof Player p) {
                return !p.getName().equals(player.getName());
            }
            return true;
        });
        if (result == null) return null;
        return (LivingEntity) result.getHitEntity();
    }

    /**
     * Get the remaining durability of the tool in the player's main hand.
     * Refer to {@link ItemUtils}{@code .getToolDurability} for details.
     *
     * @param player The player to check.
     * @return The durability of the player's main hand tool, if any.
     */
    public static Optional<Integer> getMainHandToolDurability(Player player) {
        return ItemUtils.getToolDurability(player.getInventory().getItemInMainHand());
    }

    /**
     * Check if the player, with the tool in their main hand, can break a certain block.
     * The player can break the block if there are block drops after breaking it with the tool in main hand.
     *
     * @param player The player to check.
     * @param block  The block to break.
     * @return Whether the block is breakable by the player.
     */
    public static boolean canMainHandBreakBlock(Player player, Block block) {
        return !block.getDrops(player.getInventory().getItemInMainHand(), player).isEmpty();
    }
}
