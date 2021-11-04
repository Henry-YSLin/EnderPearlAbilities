package io.github.henry_yslin.enderpearlabilities;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link Listener} with onEnable and onDisable methods, runnables management and configuration loading.
 */
@SuppressWarnings("rawtypes")
public abstract class ExtendedListener<TRunnable extends ExtendedRunnable> implements Listener {

    public final Plugin plugin;
    protected final ConfigurationSection config;
    public final List<TRunnable> runnables = Collections.synchronizedList(new ArrayList<>());

    /**
     * Create an {@link ExtendedListener} instance.
     *
     * @param plugin The owning plugin.
     * @param config The {@link ConfigurationSection} to read from, null if this instance is a template.
     */
    public ExtendedListener(Plugin plugin, @Nullable ConfigurationSection config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Populate a given {@link ConfigurationSection} with default values.
     * Works with a template instance.
     *
     * @param config The {@link ConfigurationSection} to be populated.
     */
    public abstract void setConfigDefaults(ConfigurationSection config);

    public void onEnable() {
    }

    public void onDisable() {
        synchronized (runnables) {
            for (int i = runnables.size() - 1; i >= 0; i--) {
                runnables.get(i).cancel();
            }
        }
    }
}
