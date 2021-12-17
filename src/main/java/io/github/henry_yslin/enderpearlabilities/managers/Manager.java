package io.github.henry_yslin.enderpearlabilities.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

/**
 * Controls shared behavior or interactions between different abilities.
 */
public abstract class Manager extends ManagerListener {

    protected Manager(Plugin plugin, ConfigurationSection config) {
        super(plugin, config);
    }

    public abstract String getName();
}
