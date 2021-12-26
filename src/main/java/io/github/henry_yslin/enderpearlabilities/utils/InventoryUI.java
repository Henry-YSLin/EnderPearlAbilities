package io.github.henry_yslin.enderpearlabilities.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.function.Consumer;

public class InventoryUI implements Listener {
    private final Plugin plugin;
    private final Inventory inventory;
    private final List<ItemStack> choices;
    private boolean completed = false;
    private final Consumer<ItemStack> onSelect;
    private final Runnable onCancel;

    public InventoryUI(Plugin plugin, String title, List<ItemStack> choices, Consumer<ItemStack> onSelect, Runnable onCancel) {
        this.plugin = plugin;
        inventory = Bukkit.createInventory(null, 54, title);
        this.choices = choices;
        this.onSelect = onSelect;
        this.onCancel = onCancel;

        initializeItems();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void initializeItems() {
        for (int i = 0; i < choices.size(); i++) {
            inventory.setItem(i, choices.get(i));
        }
    }

    public void openInventory(final Player player) {
        player.openInventory(inventory);
    }

    public boolean isCompleted() {
        return completed;
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;

        event.setCancelled(true);

        final ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;
        if (event.getSlot() < 0 || event.getSlot() >= choices.size()) return;
        if (completed) return;

        final Player player = (Player) event.getWhoClicked();
        completed = true;
        HandlerList.unregisterAll(this);
        onSelect.accept(choices.get(event.getSlot()));
        new BukkitRunnable() {
            @Override
            public void run() {
                player.closeInventory();
            }
        }.runTaskLater(plugin, 0);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (completed) return;
        completed = true;
        HandlerList.unregisterAll(this);
        onCancel.run();
    }

    @EventHandler
    public void onInventoryClick(final InventoryDragEvent e) {
        if (e.getInventory().equals(inventory)) {
            e.setCancelled(true);
        }
    }
}
