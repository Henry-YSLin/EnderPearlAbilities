package io.github.henry_yslin.enderpearlabilities;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public abstract class BasicExtendedListener extends ExtendedListener<BasicExtendedRunnable> {

    public BasicExtendedListener(Plugin plugin, @Nullable ConfigurationSection config) {
        super(plugin, config);
    }
}
