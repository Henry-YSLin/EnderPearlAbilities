package io.github.henry_yslin.enderpearlabilities.abilities;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public abstract class MultipleChargeAbilityWithDurationInfo extends AbilityWithDurationInfo {

    protected int charge;

    @Override
    public int getCharge() {
        return charge;
    }

    @Override
    protected void readFromConfig(ConfigurationSection config) {
        super.readFromConfig(config);
        charge = config.getInt("charge");
    }

    public MultipleChargeAbilityWithDurationInfo(Plugin plugin) {
        super(plugin);
    }
}
