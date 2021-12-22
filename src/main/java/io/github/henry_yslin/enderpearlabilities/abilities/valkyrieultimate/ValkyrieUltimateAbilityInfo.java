package io.github.henry_yslin.enderpearlabilities.abilities.valkyrieultimate;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class ValkyrieUltimateAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 40);
        config.addDefault("duration", 200);
        config.addDefault("cooldown", 1200);
    }

    @Override
    public String getCodeName() {
        return "valkyrie-ultimate";
    }

    @Override
    public String getName() {
        return "Skyward Dive";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Valkyrie";
    }

    @Override
    public String getDescription() {
        return "Launch into the air and skydive, revealing entities while in flight.";
    }

    @Override
    public String getUsage() {
        return "Right-click once to prepare for launch. Switch away from ender pearl to cancel. Right-click again to launch into the air and skydive. Living entities are pinged with flashing light while you are in flight.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    @Override
    public ValkyrieUltimateAbility createInstance(String ownerName) {
        return new ValkyrieUltimateAbility(plugin, this, ownerName);
    }

    public ValkyrieUltimateAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
