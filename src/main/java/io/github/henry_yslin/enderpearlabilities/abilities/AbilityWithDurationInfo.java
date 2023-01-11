package io.github.henry_yslin.enderpearlabilities.abilities;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public abstract class AbilityWithDurationInfo extends AbilityInfo {

    protected int chargeUp;
    protected int duration;
    protected int cooldown;

    @Override
    public int getChargeUp() {
        return chargeUp;
    }

    @Override
    public int getDuration() {
        return duration;
    }

    @Override
    public int getCooldown() {
        return cooldown;
    }

    @Override
    public int getCharge() {
        return 1;
    }

    @Override
    protected void readFromConfig(ConfigurationSection config) {
        chargeUp = config.getInt("charge-up");
        duration = config.getInt("duration");
        cooldown = config.getInt("cooldown");
    }

    public AbilityWithDurationInfo(Plugin plugin) {
        super(plugin);
    }
}
