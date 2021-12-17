package io.github.henry_yslin.enderpearlabilities;

import org.bukkit.configuration.ConfigurationSection;

public interface HasConfig {

    String getCodeName();

    boolean isConfigLoaded();

    void loadConfig(ConfigurationSection config);

    void writeConfigDefaults(ConfigurationSection config);
}
