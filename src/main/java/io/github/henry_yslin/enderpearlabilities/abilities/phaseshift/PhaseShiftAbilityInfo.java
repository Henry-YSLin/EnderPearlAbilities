package io.github.henry_yslin.enderpearlabilities.abilities.phaseshift;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class PhaseShiftAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 50);
        config.addDefault("cooldown", 400);
    }

    @Override
    public String getCodeName() {
        return "phase-shift";
    }

    @Override
    public String getName() {
        return "Phase Shift";
    }

    @Override
    public String getOrigin() {
        return "Titanfall";
    }

    @Override
    public String getDescription() {
        return "Become invulnerable and invisible by entering an alternate dimension.";
    }

    @Override
    public String getUsage() {
        return "Right click with an ender pearl to activate the ability. Right click again to exit early. You may not interact with anything while the ability is active.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.OffHand;
    }

    @Override
    public PhaseShiftAbility createInstance(String ownerName) {
        return new PhaseShiftAbility(plugin, this, ownerName);
    }

    public PhaseShiftAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
