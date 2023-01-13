package io.github.henry_yslin.enderpearlabilities.abilities.vantageultimate;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henry_yslin.enderpearlabilities.abilities.MultipleChargeAbilityWithDurationInfo;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class VantageUltimateAbilityInfo extends MultipleChargeAbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 10 * 20);
        config.addDefault("cooldown", 40 * 20);
        config.addDefault("charge", 5);
    }

    @Override
    public String getCodeName() {
        return "vantage-ultimate";
    }

    @Override
    public String getName() {
        return "Sniper's Mark";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Vantage";
    }

    @Override
    public String getDescription() {
        return "Use your custom weapon to mark enemy targets which increases their damage taken.";
    }

    @Override
    public String getUsage() {
        return "Right click to obtain a bow in off hand. Hit an entity with the bow to mark them. Marked entities will be revealed and will take increased damage from all sources. An additional wither effect will be applied if the shot is a headshot.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    @Override
    public VantageUltimateAbility createInstance(String ownerName) {
        return new VantageUltimateAbility(plugin, this, ownerName);
    }

    public VantageUltimateAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
