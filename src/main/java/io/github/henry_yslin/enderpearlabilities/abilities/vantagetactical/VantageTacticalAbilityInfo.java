package io.github.henry_yslin.enderpearlabilities.abilities.vantagetactical;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class VantageTacticalAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 20);
        config.addDefault("duration", 0);
        config.addDefault("cooldown", 40 * 20);
    }

    @Override
    public String getCodeName() {
        return "vantage-tactical";
    }

    @Override
    public String getName() {
        return "Echo Relocation";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Vantage";
    }

    @Override
    public String getDescription() {
        return "Position your winged companion Echo and then Launch toward him. Must have line of sight for Echo to launch.";
    }

    @Override
    public String getUsage() {
        return "Right click to deploy Echo or to relocate it. Sneak while right-clicking to recall Echo. While Echo is deployed, right click to launch towards it.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.OffHand;
    }

    @Override
    public VantageTacticalAbility createInstance(String ownerName) {
        return new VantageTacticalAbility(plugin, this, ownerName);
    }

    public VantageTacticalAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
