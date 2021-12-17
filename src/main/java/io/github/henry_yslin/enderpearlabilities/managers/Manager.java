package io.github.henry_yslin.enderpearlabilities.managers;

import io.github.henry_yslin.enderpearlabilities.HasConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

/**
 * Controls shared behavior or interactions between different abilities.
 */
public abstract class Manager extends ManagerListener implements HasConfig {

    protected Manager(Plugin plugin) {
        super(plugin);
    }

    boolean configLoaded = false;

    @Override
    public abstract String getCodeName();

    @Override
    public boolean isConfigLoaded() {
        return configLoaded;
    }

    @Override
    public void loadConfig(ConfigurationSection config) {
        configLoaded = true;
        readFromConfig(config);
    }

    protected abstract void readFromConfig(ConfigurationSection config);
}
