package io.github.henry_yslin.enderpearlabilities.abilities.fusetactical;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henry_yslin.enderpearlabilities.abilities.MultipleChargeAbilityWithDurationInfo;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class FuseTacticalAbilityInfo extends MultipleChargeAbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 6 * 20);
        config.addDefault("cooldown", 25 * 20);
        config.addDefault("charge", 2);
    }

    @Override
    public String getCodeName() {
        return "fuse-tactical";
    }

    @Override
    public String getName() {
        return "Knuckle Cluster";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Fuse";
    }

    @Override
    public String getDescription() {
        return "Launch a cluster bomb that continuously expels air-burst explosives on impact.";
    }

    @Override
    public String getUsage() {
        return "Right click to fire. Mini-explosives are continually expelled on impact.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.OffHand;
    }

    @Override
    public FuseTacticalAbility createInstance(String ownerName) {
        return new FuseTacticalAbility(plugin, this, ownerName);
    }

    public FuseTacticalAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
