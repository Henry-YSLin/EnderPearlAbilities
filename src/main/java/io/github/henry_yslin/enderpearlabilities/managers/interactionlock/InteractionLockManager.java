package io.github.henry_yslin.enderpearlabilities.managers.interactionlock;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.managers.Manager;
import io.github.henry_yslin.enderpearlabilities.utils.EntityUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

@Instantiable
public class InteractionLockManager extends Manager {

    private static InteractionLockManager instance = null;

    public static InteractionLockManager getInstance() {
        return instance;
    }

    @Override
    public String getCodeName() {
        return "interaction-lock";
    }

    public InteractionLockManager(Plugin plugin) {
        super(plugin);

        if (instance != null)
            throw new RuntimeException("InteractionLockManager already exists!");
        instance = this;
    }

    @Override
    protected void readFromConfig(ConfigurationSection config) {
        // no configs yet
    }

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        // no configs yet
    }

    public boolean isInteractionLocked(Player player) {
        Optional<Object> lock = EntityUtils.getMetadata(player, "interaction-lock");
        return lock.filter(o -> (boolean) o).isPresent();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            unlockInteraction(player);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isInteractionLocked(event.getPlayer())) return;
        event.setCancelled(true);
    }

    public void lockInteraction(Player player) {
        if (isInteractionLocked(player)) return;

        player.setMetadata("interaction-lock", new FixedMetadataValue(plugin, true));
    }

    public void unlockInteraction(Player player) {
        if (!isInteractionLocked(player)) return;

        player.removeMetadata("interaction-lock", plugin);
    }
}

