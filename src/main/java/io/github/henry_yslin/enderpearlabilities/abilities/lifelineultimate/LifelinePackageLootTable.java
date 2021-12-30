package io.github.henry_yslin.enderpearlabilities.abilities.lifelineultimate;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.utils.ItemStackUtils;
import io.github.henry_yslin.enderpearlabilities.utils.ListUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class LifelinePackageLootTable implements LootTable {

    static final int MAX_COST = 10;

    private List<ItemStack> generatedLoot = null;

    @NotNull
    @Override
    public Collection<ItemStack> populateLoot(@NotNull Random random, @NotNull LootContext context) {
        if (context.getKiller() == null) return new ArrayList<>();
        PlayerInventory inventory = context.getKiller().getInventory();
        List<ItemStack> finalLoot = new ArrayList<>();
        List<LootChoice> foodChoices = getFoodLootChoices(inventory);
        List<LootChoice> armorChoices = getArmorLootChoices(inventory);
        List<LootChoice> enchantmentChoices = getEnchantmentLootChoices(inventory);
        List<Integer> sections = new ArrayList<>(List.of(1, 2, 3));
        int costRemaining = MAX_COST;
        while (costRemaining > 0) {
            int section;
            if (sections.isEmpty()) {
                section = (int) (Math.random() * 3 + 1);
            } else {
                section = ListUtils.getRandom(sections);
                sections.remove(Integer.valueOf(section));
            }
            int finalCostRemaining = costRemaining;
            List<LootChoice> remainingChoices;
            if (section == 1) {
                remainingChoices = foodChoices.stream().filter(c -> c.cost <= finalCostRemaining).toList();
            } else if (section == 2) {
                remainingChoices = armorChoices.stream().filter(c -> c.cost <= finalCostRemaining).toList();
            } else if (section == 3) {
                remainingChoices = enchantmentChoices.stream().filter(c -> c.cost <= finalCostRemaining).toList();
            } else {
                remainingChoices = foodChoices.stream().filter(c -> c.cost <= finalCostRemaining).toList();
            }
            if (remainingChoices.isEmpty()) continue;
            LootChoice choice = ListUtils.getRandom(remainingChoices);
            finalLoot.add(choice.item);
            costRemaining -= choice.cost;
        }
        generatedLoot = finalLoot;
        return generatedLoot;
    }

    private List<LootChoice> getArmorLootChoices(PlayerInventory inventory) {
        List<LootChoice> choices = new ArrayList<>();
        List<Integer> armorLevels = new ArrayList<>();

        armorLevels.add(ItemStackUtils.getArmorLevel(inventory.getHelmet()));
        armorLevels.add(ItemStackUtils.getArmorLevel(inventory.getChestplate()));
        armorLevels.add(ItemStackUtils.getArmorLevel(inventory.getLeggings()));
        armorLevels.add(ItemStackUtils.getArmorLevel(inventory.getBoots()));

        int maxLevel = armorLevels.stream().mapToInt(Integer::intValue).max().orElse(0);
        if (maxLevel == 0) {
            choices.add(new LootChoice(new ItemStack(Material.LEATHER_HELMET), 1, 1));
            choices.add(new LootChoice(new ItemStack(Material.LEATHER_CHESTPLATE), 2, 1));
            choices.add(new LootChoice(new ItemStack(Material.LEATHER_LEGGINGS), 2, 1));
            choices.add(new LootChoice(new ItemStack(Material.LEATHER_BOOTS), 1, 1));
        } else {
            if (armorLevels.get(0) != maxLevel)
                choices.add(new LootChoice(ItemStackUtils.getHelmetByLevel(Math.min(4, maxLevel)), 1, maxLevel));
            if (armorLevels.get(1) != maxLevel)
                choices.add(new LootChoice(ItemStackUtils.getChestplateByLevel(Math.min(4, maxLevel)), 2, maxLevel));
            if (armorLevels.get(2) != maxLevel)
                choices.add(new LootChoice(ItemStackUtils.getLeggingsByLevel(Math.min(4, maxLevel)), 2, maxLevel));
            if (armorLevels.get(3) != maxLevel)
                choices.add(new LootChoice(ItemStackUtils.getBootsByLevel(Math.min(4, maxLevel)), 1, maxLevel));
            if (choices.isEmpty()) {
                choices.add(new LootChoice(ItemStackUtils.getHelmetByLevel(Math.min(4, maxLevel + 1)), 1, maxLevel + 1));
                choices.add(new LootChoice(ItemStackUtils.getChestplateByLevel(Math.min(4, maxLevel + 1)), 2, maxLevel + 1));
                choices.add(new LootChoice(ItemStackUtils.getLeggingsByLevel(Math.min(4, maxLevel + 1)), 2, maxLevel + 1));
                choices.add(new LootChoice(ItemStackUtils.getBootsByLevel(Math.min(4, maxLevel + 1)), 1, maxLevel + 1));
            }
        }
        return choices;
    }

    private List<LootChoice> getEnchantmentLootChoices(PlayerInventory inventory) {
        List<LootChoice> lootChoices = Arrays.stream(inventory.getContents())
                .filter(Objects::nonNull)
                .flatMap(itemStack -> itemStack.getEnchantments().entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey))
                .entrySet().stream()
                .map(entry -> new EnchantmentTuple(
                        entry.getKey(),
                        entry.getValue().stream()
                                .filter(e -> e.getValue() < e.getKey().getMaxLevel())
                                .max(Comparator.comparingInt(Map.Entry::getValue))
                                .map(Map.Entry::getValue)
                                .orElse(-1)
                ))
                .filter(tuple -> tuple.level >= 0)
                .map(tuple -> {
                    ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
                    ItemStackUtils.storeEnchantment(item, tuple.enchantment, tuple.level, false);
                    return new LootChoice(item, 1, 2 + tuple.level * 2);
                })
                .collect(Collectors.toList());
        if (lootChoices.isEmpty()) {
            ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
            ItemStackUtils.storeEnchantment(item, Enchantment.PROTECTION_ENVIRONMENTAL, Enchantment.PROTECTION_ENVIRONMENTAL.getStartLevel(), false);
            lootChoices.add(new LootChoice(item, 1, 2 + Enchantment.PROTECTION_ENVIRONMENTAL.getStartLevel() * 2));
            ItemStack item2 = new ItemStack(Material.ENCHANTED_BOOK);
            ItemStackUtils.storeEnchantment(item2, Enchantment.DURABILITY, Enchantment.DURABILITY.getStartLevel(), false);
            lootChoices.add(new LootChoice(item2, 1, 2 + Enchantment.DURABILITY.getStartLevel() * 2));
        }
        return lootChoices;
    }

    private List<LootChoice> getFoodLootChoices(PlayerInventory inventory) {
        List<LootChoice> choices = new ArrayList<>();
        choices.add(new LootChoice(new ItemStack(Material.COOKED_BEEF, 7), 1, 2));
        choices.add(new LootChoice(new ItemStack(Material.COOKED_PORKCHOP, 10), 1, 2));
        choices.add(new LootChoice(new ItemStack(Material.COOKED_CHICKEN, 10), 1, 2));
        choices.add(new LootChoice(new ItemStack(Material.COOKED_MUTTON, 10), 1, 2));
        choices.add(new LootChoice(new ItemStack(Material.COOKED_RABBIT, 10), 1, 2));
        choices.add(new LootChoice(new ItemStack(Material.COOKED_SALMON, 17), 1, 1));
        choices.add(new LootChoice(new ItemStack(Material.COOKED_COD, 17), 1, 1));
        choices.add(new LootChoice(new ItemStack(Material.DRIED_KELP, 17), 1, 1));
        choices.add(new LootChoice(new ItemStack(Material.GOLDEN_APPLE), 1, 4));
        choices.add(new LootChoice(new ItemStack(Material.GOLDEN_CARROT, 2), 1, 3));
        return choices;
    }

    @Override
    public void fillInventory(@NotNull Inventory inventory, @NotNull Random random, @NotNull LootContext context) {
        if (generatedLoot == null || generatedLoot.isEmpty())
            throw new IllegalStateException("Loot has not been generated yet!");
        List<ItemStack> food = generatedLoot.stream().filter(l -> l.getType().isEdible()).collect(Collectors.toList());
        List<ItemStack> enchantments = generatedLoot.stream().filter(l -> l.getType() == Material.ENCHANTED_BOOK).collect(Collectors.toList());
        List<ItemStack> armor = generatedLoot.stream().filter(l -> !l.getType().isEdible() && l.getType() != Material.ENCHANTED_BOOK).collect(Collectors.toList());
        Collections.shuffle(food, random);
        Collections.shuffle(enchantments, random);
        Collections.shuffle(armor, random);
        placeCentered(inventory, armor, 0);
        placeCentered(inventory, enchantments, 1);
        placeCentered(inventory, food, 2);
    }

    /**
     * Place the list of items into the specified row of the inventory, centering the items.
     * If there is an even number of items, leave the middle slot blank.
     */
    private void placeCentered(Inventory inventory, List<ItemStack> items, int row) {
        if (items.isEmpty())
            return;
        if (items.size() > 9)
            items = items.subList(0, 9);
        int start = row * 9 + 4 - items.size() / 2;
        if (items.size() % 2 == 0) {
            for (int i = 0; i < items.size() / 2; i++) {
                inventory.setItem(start + i, items.get(i));
            }
            for (int i = items.size() / 2; i < items.size(); i++) {
                inventory.setItem(start + i + 1, items.get(i));
            }
        } else {
            for (int i = 0; i < items.size(); i++) {
                inventory.setItem(start + i, items.get(i));
            }
        }
    }

    @NotNull
    @Override
    public NamespacedKey getKey() {
        return new NamespacedKey(EnderPearlAbilities.getInstance(), "lifeline_ultimate_package");
    }

    private static record LootChoice(ItemStack item, int weight, int cost) {
    }

    private static record EnchantmentTuple(Enchantment enchantment, int level) {
    }
}
