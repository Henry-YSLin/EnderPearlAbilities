package io.github.henry_yslin.enderpearlabilities.abilities.ashultimate;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class AshUltimateAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 20 * 20);
        config.addDefault("cooldown", 60 * 20);
    }

    @Override
    public String getCodeName() {
        return "ash-ultimate";
    }

    @Override
    public String getName() {
        return "Phase Breach";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Ash";
    }

    @Override
    public String getDescription() {
        return "Tear open a one-way portal to a targeted location and immediately enter it.";
    }

    @Override
    public String getUsage() {
        return "Right click to show targeting UI. Right click again to activate. Switch away from ender pearl or click an invalid location to cancel.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    @Override
    public AshUltimateAbility createInstance(String ownerName) {
        return new AshUltimateAbility(plugin, this, ownerName);
    }

    public AshUltimateAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
