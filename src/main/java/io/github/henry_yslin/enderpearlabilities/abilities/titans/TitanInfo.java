package io.github.henry_yslin.enderpearlabilities.abilities.titans;

import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public abstract class TitanInfo extends AbilityWithDurationInfo {

    protected int titanAbilityChargeUp;
    protected int titanAbilityCooldown;

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 0);
        config.addDefault("cooldown", 300 * 20);
    }

    @Override
    public String getOrigin() {
        return "Titanfall";
    }

    @Override
    public String getDescription() {
        return "Deploy and pilot a titan for combat.\nTitan ability: " + getTitanAbilityDescription() + "\nPassive ability: dealing damage reduces titan cooldown.";
    }

    @Override
    public String getUsage() {
        return "Right click to summon a titan at your location. Use vehicle controls to mount and dismount the titan. While mounted, right click to switch between attack and move mode. Left click in attack mode to activate titan ability. Left click in move mode to jump. You are invincible while controlling the titan, but you will be ejected upwards when the titan is destroyed.";
    }

    public abstract String getTitanAbilityDescription();

    public abstract String getTitanAbilityName();

    public int getTitanAbilityChargeUp() {
        return titanAbilityChargeUp;
    }

    public int getTitanAbilityCooldown() {
        return titanAbilityCooldown;
    }

    @Override
    protected void readFromConfig(ConfigurationSection config) {
        super.readFromConfig(config);
        titanAbilityChargeUp = config.getInt("titan-ability-charge-up");
        titanAbilityCooldown = config.getInt("titan-ability-cooldown");
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    public TitanInfo(Plugin plugin) {
        super(plugin);
    }
}
