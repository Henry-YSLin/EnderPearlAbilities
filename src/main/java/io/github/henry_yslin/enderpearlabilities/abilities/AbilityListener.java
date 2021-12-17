package io.github.henry_yslin.enderpearlabilities.abilities;

import io.github.henry_yslin.enderpearlabilities.ExtendedListener;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public abstract class AbilityListener extends ExtendedListener<AbilityRunnable, AbilityListener> {

    public AbilityListener(Plugin plugin, @Nullable ConfigurationSection config) {
        super(plugin, config);
    }
}
