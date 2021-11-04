package io.github.henry_yslin.enderpearlabilities.managers;

import io.github.henry_yslin.enderpearlabilities.ExtendedListener;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

/**
 * Controls shared behavior or interactions between different abilities.
 */
public abstract class Manager extends ExtendedListener<ManagerRunnable> {

    public Manager(Plugin plugin, ConfigurationSection config) {
        super(plugin, config);
    }
}
