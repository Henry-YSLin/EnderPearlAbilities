package io.github.henry_yslin.enderpearlabilities.abilities;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Controls the behavior and description of a specific ability for one specified player.
 */
public abstract class Ability<TInfo extends AbilityInfo> extends AbilityListener {

    protected TInfo info;
    protected final String ownerName;
    protected Player player;
    protected AbilityCooldown cooldown;

    /**
     * Create an {@link Ability} instance.
     *
     * @param plugin    The owning plugin.
     * @param ownerName Name of the player who owns this ability, null if this instance is a template.
     */
    public Ability(@NotNull Plugin plugin, @NotNull TInfo info, @NotNull String ownerName) {
        super(plugin);
        this.ownerName = ownerName;
        this.info = info;
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
     */
    public TInfo getInfo() {
        return info;
    }

    /**
     * Get the name of the player who owns this ability.
     */
    public String getOwnerName() {
        return ownerName;
    }

    /**
     * Get the player who owns this ability.
     *
     * @return The player who owns this ability, or null if the player is offline.
     */
    @Nullable
    public Player getPlayer() {
        return player;
    }

    /**
     * Get the cooldown instance of this ability.
     * <p>
     * The cooldown instance is linked to this ability instance, allowing direct control of the cooldown.
     */
    public AbilityCooldown getCooldown() {
        return cooldown;
    }

    public abstract boolean isActive();

    public abstract boolean isChargingUp();

    public void onEnable() {
        if (ownerName == null) return;
        player = plugin.getServer().getPlayer(ownerName);
        if (player != null) {
            cooldown = new AbilityCooldown(this, player);
        }
        super.onEnable();
    }
}
