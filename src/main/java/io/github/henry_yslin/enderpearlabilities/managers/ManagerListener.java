package io.github.henry_yslin.enderpearlabilities.managers;

import io.github.henry_yslin.enderpearlabilities.ExtendedListener;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public abstract class ManagerListener extends ExtendedListener<ManagerRunnable, ManagerListener> {

    public ManagerListener(Plugin plugin, @Nullable ConfigurationSection config) {
        super(plugin, config);
    }
}
