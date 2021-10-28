package io.github.henryyslin.enderpearlabilities.utils;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Optional;

public class PlayerUtils {

    /**
     * Remove an ender pearl from the player's inventory as a result of ability activation.
     * <p>
     * Note: This method does not simply remove an ender pearl from the player's selected hot bar slot.
     * Instead, it first attempts to remove a pearl from the player's backpack, only falling back to the player's
     * main or off hand if there are no pearls in the backpack. This behavior is for the player's convenience,
     * so that they don't need to refill their main/off hands very often.
     *
     * @param player The player to remove an ender pearl from.
     * @return Whether the ender pearl is successfully removed.
     */
    public static boolean consumeEnderPearl(Player player) {
        if (player.getGameMode() != GameMode.CREATIVE) {
            HashMap<Integer, ItemStack> remainingItems = player.getInventory().removeItem(new ItemStack(Material.ENDER_PEARL, 1));
            player.updateInventory();
            if (!remainingItems.isEmpty()) {
                ItemStack heldItem = player.getInventory().getItemInMainHand();
                if (heldItem.getType() == Material.ENDER_PEARL) {
                    heldItem.setAmount(heldItem.getAmount() - 1);
                    if (heldItem.getAmount() == 0) {
                        heldItem.setType(Material.AIR);
                    }
                    player.getInventory().setItemInMainHand(heldItem);
                } else {
                    heldItem = player.getInventory().getItemInOffHand();
                    if (heldItem.getType() == Material.ENDER_PEARL) {
                        heldItem.setAmount(heldItem.getAmount() - 1);
                        if (heldItem.getAmount() == 0) {
                            heldItem.setType(Material.AIR);
                        }
                        player.getInventory().setItemInOffHand(heldItem);
                    } else {
                        return false;
                    }
                }
            }
        }
        return true;
    }

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
     * Refer to {@link ItemStackUtils}{@code .getToolDurability} for details.
     *
     * @param player The player to check.
     * @return The durability of the player's main hand tool, if any.
     */
    public static Optional<Integer> getMainHandToolDurability(Player player) {
        return ItemStackUtils.getToolDurability(player.getInventory().getItemInMainHand());
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
