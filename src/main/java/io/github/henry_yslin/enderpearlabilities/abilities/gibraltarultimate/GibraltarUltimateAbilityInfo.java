package io.github.henry_yslin.enderpearlabilities.abilities.gibraltarultimate;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class GibraltarUltimateAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 6 * 20);
        config.addDefault("cooldown", 180 * 20);
    }

    @Override
    public String getCodeName() {
        return "gibraltar-ultimate";
    }

    @Override
    public String getName() {
        return "Defensive Bombardment";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Gibraltar";
    }

    @Override
    public String getDescription() {
        return "Call in a concentrated mortar strike on a marked position.";
    }

    @Override
    public String getUsage() {
        return "Right click to throw a flare that marks a radius for continuous bombardment.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    @Override
    public GibraltarUltimateAbility createInstance(String ownerName) {
        return new GibraltarUltimateAbility(plugin, this, ownerName);
    }

    public GibraltarUltimateAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
