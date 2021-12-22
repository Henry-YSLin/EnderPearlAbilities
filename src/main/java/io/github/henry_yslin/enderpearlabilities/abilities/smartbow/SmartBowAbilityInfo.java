package io.github.henry_yslin.enderpearlabilities.abilities.smartbow;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithMagazineInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class SmartBowAbilityInfo extends AbilityWithMagazineInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 10);
        config.addDefault("magazine-size", 12);
        config.addDefault("base-cooldown", 5 * 20);
        config.addDefault("cooldown-per-shot", 25);
    }

    @Override
    public String getCodeName() {
        return "smart-bow";
    }

    @Override
    public String getName() {
        return "Smart Bow";
    }

    @Override
    public String getOrigin() {
        return "Titanfall";
    }

    @Override
    public String getDescription() {
        return "Enhance your bow-type weapons with advanced automatic aim correction that greatly increases the chance of hitting your target.";
    }

    @Override
    public String getUsage() {
        return "Right click with an ender pearl to activate. Right click again to cancel. Cooldown depends on the number of shots fired.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.OffHand;
    }

    @Override
    public SmartBowAbility createInstance(String ownerName) {
        return new SmartBowAbility(plugin, this, ownerName);
    }

    public SmartBowAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
