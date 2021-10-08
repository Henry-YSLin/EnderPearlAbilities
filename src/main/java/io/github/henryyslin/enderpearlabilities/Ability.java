package io.github.henryyslin.enderpearlabilities;

import io.github.henryyslin.enderpearlabilities.utils.AbilityRunnable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Ability implements Listener {
    public final Plugin plugin;
    public final String ownerName;
    protected final ConfigurationSection config;
    public final List<AbilityRunnable> runnables = Collections.synchronizedList(new ArrayList<>());

    public Ability(Plugin plugin, String ownerName, ConfigurationSection config) {
        this.plugin = plugin;
        this.ownerName = ownerName;
        this.config = config;
    }

    public abstract void setConfigDefaults(ConfigurationSection config);

    public abstract AbilityInfo getInfo();

    public void onEnable() {
    }

    public void onDisable() {
        synchronized (runnables) {
            for (AbilityRunnable runnable : runnables) {
                runnable.cancel();
            }
        }
    }
}
