package io.github.henry_yslin.enderpearlabilities.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.Optional;

public class ItemStackUtils {

    /**
     * Check whether an {@link ItemStack} has durability.
     *
     * @param itemStack The {@link ItemStack} to check.
     * @return Whether the {@link ItemStack} has durability.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean hasDurability(ItemStack itemStack) {
        if (itemStack == null) return false;
        return itemStack.getType().getMaxDurability() != 0;
    }

    /**
     * Apply the specified damage amount to an {@link ItemStack}.
     * The {@link ItemStack}'s damage is clamped between 0 and its maximum durability.
     *
     * @param itemStack The {@link ItemStack} to damage.
     * @param damage    The damage to apply.
     */
    public static void damageTool(ItemStack itemStack, int damage) {
        if (itemStack == null) return;
        if (!hasDurability(itemStack)) return;
        if (!(itemStack.getItemMeta() instanceof Damageable damageable)) return;
        int maxDamage = itemStack.getType().getMaxDurability() - 1;
        if (damageable.hasDamage())
            damageable.setDamage(Math.max(0, Math.min(maxDamage, damageable.getDamage() + damage)));
        else
            damageable.setDamage(Math.max(0, Math.min(maxDamage, damage)));
        itemStack.setItemMeta(damageable);
    }

    /**
     * Get the remaining uses (durability) of a tool as an {@link ItemStack}.
     *
     * @param itemStack The {@link ItemStack} to check.
     * @return The durability, if any.
     */
    public static Optional<Integer> getToolDurability(ItemStack itemStack) {
        if (!hasDurability(itemStack)) return Optional.empty();
        if (!(itemStack.getItemMeta() instanceof Damageable damageable)) return Optional.empty();
        if (damageable.hasDamage()) {
            return Optional.of(itemStack.getType().getMaxDurability() - damageable.getDamage());
        } else {
            return Optional.of((int) itemStack.getType().getMaxDurability());
        }
    }

    /**
     * Store an {@link Enchantment} on an {@link ItemStack}.
     *
     * @param itemStack              The {@link ItemStack} to store the {@link Enchantment} on.
     * @param enchantment            The {@link Enchantment} to store.
     * @param level                  The level of the {@link Enchantment}.
     * @param ignoreLevelRestriction Whether to ignore the level restriction.
     * @return Whether the {@link Enchantment} was stored successfully.
     */
    public static boolean storeEnchantment(ItemStack itemStack, Enchantment enchantment, int level, boolean ignoreLevelRestriction) {
        if (itemStack == null) return false;
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemStack.getItemMeta();
        if (meta == null) return false;
        meta.addStoredEnchant(enchantment, level, ignoreLevelRestriction);
        itemStack.setItemMeta(meta);
        return true;
    }

    public static int getArmorLevel(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 0;
        if (item.getType() == Material.LEATHER_HELMET
                || item.getType() == Material.LEATHER_CHESTPLATE
                || item.getType() == Material.LEATHER_LEGGINGS
                || item.getType() == Material.LEATHER_BOOTS) return 1;
        if (item.getType() == Material.CHAINMAIL_HELMET
                || item.getType() == Material.CHAINMAIL_CHESTPLATE
                || item.getType() == Material.CHAINMAIL_LEGGINGS
                || item.getType() == Material.CHAINMAIL_BOOTS) return 2;
        if (item.getType() == Material.IRON_HELMET
                || item.getType() == Material.IRON_CHESTPLATE
                || item.getType() == Material.IRON_LEGGINGS
                || item.getType() == Material.IRON_BOOTS) return 3;
        if (item.getType() == Material.GOLDEN_HELMET
                || item.getType() == Material.GOLDEN_CHESTPLATE
                || item.getType() == Material.GOLDEN_LEGGINGS
                || item.getType() == Material.GOLDEN_BOOTS) return 4;
        if (item.getType() == Material.DIAMOND_HELMET
                || item.getType() == Material.DIAMOND_CHESTPLATE
                || item.getType() == Material.DIAMOND_LEGGINGS
                || item.getType() == Material.DIAMOND_BOOTS) return 5;
        if (item.getType() == Material.NETHERITE_HELMET
                || item.getType() == Material.NETHERITE_CHESTPLATE
                || item.getType() == Material.NETHERITE_LEGGINGS
                || item.getType() == Material.NETHERITE_BOOTS) return 5;
        return 0;
    }

    public static ItemStack getHelmetByLevel(int level) {
        return switch (level) {
            case 1 -> new ItemStack(Material.LEATHER_HELMET);
            case 2 -> new ItemStack(Material.CHAINMAIL_HELMET);
            case 3 -> new ItemStack(Material.IRON_HELMET);
            case 4 -> new ItemStack(Material.GOLDEN_HELMET);
            case 5 -> new ItemStack(Material.DIAMOND_HELMET);
            case 6 -> new ItemStack(Material.NETHERITE_HELMET);
            default -> new ItemStack(Material.AIR);
        };
    }

    public static ItemStack getChestplateByLevel(int level) {
        return switch (level) {
            case 1 -> new ItemStack(Material.LEATHER_CHESTPLATE);
            case 2 -> new ItemStack(Material.CHAINMAIL_CHESTPLATE);
            case 3 -> new ItemStack(Material.IRON_CHESTPLATE);
            case 4 -> new ItemStack(Material.GOLDEN_CHESTPLATE);
            case 5 -> new ItemStack(Material.DIAMOND_CHESTPLATE);
            case 6 -> new ItemStack(Material.NETHERITE_CHESTPLATE);
            default -> new ItemStack(Material.AIR);
        };
    }

    public static ItemStack getLeggingsByLevel(int level) {
        return switch (level) {
            case 1 -> new ItemStack(Material.LEATHER_LEGGINGS);
            case 2 -> new ItemStack(Material.CHAINMAIL_LEGGINGS);
            case 3 -> new ItemStack(Material.IRON_LEGGINGS);
            case 4 -> new ItemStack(Material.GOLDEN_LEGGINGS);
            case 5 -> new ItemStack(Material.DIAMOND_LEGGINGS);
            case 6 -> new ItemStack(Material.NETHERITE_LEGGINGS);
            default -> new ItemStack(Material.AIR);
        };
    }

    public static ItemStack getBootsByLevel(int level) {
        return switch (level) {
            case 1 -> new ItemStack(Material.LEATHER_BOOTS);
            case 2 -> new ItemStack(Material.CHAINMAIL_BOOTS);
            case 3 -> new ItemStack(Material.IRON_BOOTS);
            case 4 -> new ItemStack(Material.GOLDEN_BOOTS);
            case 5 -> new ItemStack(Material.DIAMOND_BOOTS);
            case 6 -> new ItemStack(Material.NETHERITE_BOOTS);
            default -> new ItemStack(Material.AIR);
        };
    }
}
