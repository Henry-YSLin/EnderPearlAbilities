package io.github.henryyslin.enderpearlabilities.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

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
}
