package io.github.henry_yslin.enderpearlabilities.abilities;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public abstract class AbilityWithMagazineInfo extends AbilityInfo {

    protected int chargeUp;
    protected int magazineSize;
    protected int baseCooldown;
    protected int cooldownPerShot;

    @Override
    public int getChargeUp() {
        return chargeUp;
    }

    @Override
    public int getDuration() {
        return getMagazineSize();
    }

    @Override
    public int getCooldown() {
        return getBaseCooldown() + getCooldownPerShot() * getMagazineSize();
    }

    public int getMagazineSize() {
        return magazineSize;
    }

    public int getBaseCooldown() {
        return baseCooldown;
    }

    public int getCooldownPerShot() {
        return cooldownPerShot;
    }

    @Override
    protected void readFromConfig(ConfigurationSection config) {
        chargeUp = config.getInt("charge-up");
        magazineSize = config.getInt("magazine-size");
        baseCooldown = config.getInt("base-cooldown");
        cooldownPerShot = config.getInt("cooldown-per-shot");
    }

    public AbilityWithMagazineInfo(Plugin plugin) {
        super(plugin);
    }
}
