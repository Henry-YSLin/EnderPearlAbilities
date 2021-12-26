package io.github.henry_yslin.enderpearlabilities.abilities.lobaultimate;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class LobaUltimateAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 3 * 20);
        config.addDefault("duration", 60 * 20);
        config.addDefault("cooldown", 120 * 20);
    }

    @Override
    public String getCodeName() {
        return "loba-ultimate";
    }

    @Override
    public String getName() {
        return "Black Market Boutique";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Loba";
    }

    @Override
    public String getDescription() {
        return "Place a portable device that allows you to teleport nearby ores to your inventory.";
    }

    @Override
    public String getUsage() {
        return "Right click with an ender pearl to place the black market boutique. When it is ready, right click on the boutique and choose an ore type to teleport. The boutique will disappear when all charges are used up.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    @Override
    public LobaUltimateAbility createInstance(String ownerName) {
        return new LobaUltimateAbility(plugin, this, ownerName);
    }

    public LobaUltimateAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
