package io.github.henry_yslin.enderpearlabilities.abilities;

import io.github.henry_yslin.enderpearlabilities.ExtendedListener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public abstract class AbilityListener extends ExtendedListener<AbilityRunnable, AbilityListener> {

    public AbilityListener(@NotNull Plugin plugin) {
        super(plugin);
    }
}
