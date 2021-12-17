package io.github.henry_yslin.enderpearlabilities.managers;

import io.github.henry_yslin.enderpearlabilities.ExtendedListener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public abstract class ManagerListener extends ExtendedListener<ManagerRunnable, ManagerListener> {

    public ManagerListener(@NotNull Plugin plugin) {
        super(plugin);
    }
}
