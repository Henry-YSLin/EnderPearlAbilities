package io.github.henryyslin.enderpearlabilities.abilities;

import io.github.henryyslin.enderpearlabilities.utils.AbilityRunnable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Controls the behavior and description of a specific ability for one specified player.
 * Construct this class with a null {@code ownerName} to read description without enabling the ability.
 */
public abstract class Ability implements Listener {

    public final Plugin plugin;
    public final String ownerName;
    protected final ConfigurationSection config;
    public final List<AbilityRunnable> runnables = Collections.synchronizedList(new ArrayList<>());
    public Player player;
    public AbilityCooldown cooldown;

    /**
     * Create an {@link Ability} instance.
     *
     * @param plugin    The owning plugin.
     * @param ownerName Name of the player who owns this ability, null if this instance is a template.
     * @param config    The {@link ConfigurationSection} to read {@link AbilityInfo} from, null if this instance is created to read constants only.
     */
    public Ability(Plugin plugin, @Nullable String ownerName, @Nullable ConfigurationSection config) {
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

    /**
     * Populate a given {@link ConfigurationSection} with default values.
     * Works with a template instance.
     *
     * @param config The {@link ConfigurationSection} to be populated.
     */
    public abstract void setConfigDefaults(ConfigurationSection config);

    /**
     * Get an {@link AbilityInfo} containing descriptions of this ability.
     * Some parts of the description are not constant and change according to the configs supplied.
     *
     * @return A cached instance of {@link AbilityInfo}.
     */
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
