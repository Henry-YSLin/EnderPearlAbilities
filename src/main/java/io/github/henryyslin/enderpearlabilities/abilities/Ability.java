package io.github.henryyslin.enderpearlabilities.abilities;

import io.github.henryyslin.enderpearlabilities.utils.AbilityRunnable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Ability implements Listener {
    public final Plugin plugin;
    public final String ownerName;
    protected final ConfigurationSection config;
    public final List<AbilityRunnable> runnables = Collections.synchronizedList(new ArrayList<>());
    public Player player;
    public AbilityCooldown cooldown;

    public Ability(Plugin plugin, String ownerName, ConfigurationSection config) {
        this.plugin = plugin;
        this.ownerName = ownerName;
        this.config = config;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (ownerName == null) return;
        Player player = event.getPlayer();
        if (player.getName().equals(ownerName)) {
            this.player = player;
            cooldown = new AbilityCooldown(this, player);
        }
    }

    public abstract void setConfigDefaults(ConfigurationSection config);

    public abstract AbilityInfo getInfo();

    public void onEnable() {
        if (ownerName == null) return;
        player = plugin.getServer().getPlayer(ownerName);
        if (player != null) {
            cooldown = new AbilityCooldown(this, player);
        }
    }

    public void onDisable() {
        synchronized (runnables) {
            for (int i = runnables.size() - 1; i >= 0; i--) {
                runnables.get(i).cancel();
            }
        }
    }
}
