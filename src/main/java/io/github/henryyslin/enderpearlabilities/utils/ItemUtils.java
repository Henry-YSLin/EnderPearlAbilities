package io.github.henryyslin.enderpearlabilities.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.Optional;

public class ItemUtils {
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean hasDurability(ItemStack itemStack) {
        if (itemStack == null) return false;
        return itemStack.getType().getMaxDurability() != 0;
    }

    public static void damageTool(ItemStack itemStack, int damage) {
        if (!hasDurability(itemStack)) return;
        if (!(itemStack.getItemMeta() instanceof Damageable damageable)) return;
        int maxDamage = itemStack.getType().getMaxDurability() - 1;
        if (damageable.hasDamage())
            damageable.setDamage(Math.min(maxDamage, damageable.getDamage() + damage));
        else
            damageable.setDamage(Math.min(maxDamage, damage));
    }

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
