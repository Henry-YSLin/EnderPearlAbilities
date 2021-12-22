package io.github.henry_yslin.enderpearlabilities.abilities.octanetactical;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class OctaneTacticalAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 200);
        config.addDefault("cooldown", 60);
    }

    @Override
    public String getCodeName() {
        return "octane-tactical";
    }

    @Override
    public String getName() {
        return "Stim";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Octane";
    }

    @Override
    public String getDescription() {
        return "Boost sprinting and jumping and cancel slowing effects. Costs health to use.\nPassive ability: Automatically restores health over time.";
    }

    @Override
    public String getUsage() {
        return "Right click with an ender pearl to activate the ability.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.OffHand;
    }

    @Override
    public OctaneTacticalAbility createInstance(String ownerName) {
        return new OctaneTacticalAbility(plugin, this, ownerName);
    }

    public OctaneTacticalAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
