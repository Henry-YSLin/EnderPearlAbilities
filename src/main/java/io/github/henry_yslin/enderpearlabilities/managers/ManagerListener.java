package io.github.henry_yslin.enderpearlabilities.managers;

import io.github.henry_yslin.enderpearlabilities.ExtendedListener;
import org.bukkit.plugin.Plugin;

public abstract class ManagerListener extends ExtendedListener<ManagerRunnable, ManagerListener> {

    public ManagerListener(Plugin plugin) {
        super(plugin);
    }
}
