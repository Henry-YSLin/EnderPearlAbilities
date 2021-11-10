package io.github.henry_yslin.enderpearlabilities.managers.shield;

import io.github.henry_yslin.enderpearlabilities.managers.Manager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShieldManager extends Manager {

    @Override
    public String getName() {
        return "shield";
    }

    private static ShieldManager instance = null;

    public static ShieldManager getInstance() {
        return instance;
    }

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        super.setConfigDefaults(config);
    }

    public ShieldManager(Plugin plugin, ConfigurationSection config) {
        super(plugin, config);

        if (config == null) return; // do not assign instance if config is null since this is a template

        if (instance != null)
            throw new RuntimeException("ShieldManager already exists!");
        instance = this;
    }

    private final List<Shield> shields = Collections.synchronizedList(new ArrayList<>());
    private ShieldRunnable shieldRunnable;

    public List<Shield> getShields() {
        return shields;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        (shieldRunnable = new ShieldRunnable()).runTaskTimer(this, 0, 1);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (shieldRunnable != null && !shieldRunnable.isCancelled())
            shieldRunnable.cancel();
    }

    public void addShield(Shield shield) {
        shields.add(shield);
    }

    public boolean removeShield(Shield shield) {
        return shields.remove(shield);
    }
}

