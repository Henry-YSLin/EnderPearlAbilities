package io.github.henry_yslin.enderpearlabilities.abilities;

import io.github.henry_yslin.enderpearlabilities.ExtendedListener;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

/**
 * Controls the behavior and description of a specific ability for one specified player.
 * Construct this class with a null {@code ownerName} to read description without enabling the ability.
 */
public abstract class Ability extends ExtendedListener<AbilityRunnable> {

    public final String ownerName;
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
        super(plugin, config);
        this.ownerName = ownerName;
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
        super.onEnable();
    }
}
