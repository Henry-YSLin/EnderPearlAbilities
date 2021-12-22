package io.github.henry_yslin.enderpearlabilities.abilities.bangaloreultimate;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class BangaloreUltimateAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 5 * 20);
        config.addDefault("duration", 5 * 20);
        config.addDefault("cooldown", 180 * 20);
    }

    @Override
    public String getCodeName() {
        return "bangalore-ultimate";
    }

    @Override
    public String getName() {
        return "Rolling Thunder";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Bangalore";
    }

    @Override
    public String getDescription() {
        return "Call in an artillery strike that slowly creeps across the landscape.";
    }

    @Override
    public String getUsage() {
        return "Right click to throw a flare. Several rows of missiles will then land sequentially in front of the flare and stick for a while before exploding, slowing and blinding entities.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    @Override
    public BangaloreUltimateAbility createInstance(String ownerName) {
        return new BangaloreUltimateAbility(plugin, this, ownerName);
    }

    public BangaloreUltimateAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
