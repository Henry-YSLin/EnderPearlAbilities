package io.github.henry_yslin.enderpearlabilities.abilities;

import io.github.henry_yslin.enderpearlabilities.HasConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

/**
 * A class storing the information required to instantiate an {@link Ability} and generate an ability description.
 * The {@code codeName} of {@link AbilityInfo} should be constant while other fields can be changed via config.
 */
public abstract class AbilityInfo implements HasConfig {

    protected final Plugin plugin;

    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public abstract String getCodeName();

    public abstract String getName();

    public abstract String getOrigin();

    public abstract String getDescription();

    public abstract String getUsage();

    public abstract ActivationHand getActivation();

    public abstract int getChargeUp();

    public abstract int getDuration();

    public abstract int getCooldown();

    public abstract Ability createInstance(String ownerName);

    public AbilityInfo(Plugin plugin) {
        this.plugin = plugin;
    }

    boolean configLoaded = false;

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
