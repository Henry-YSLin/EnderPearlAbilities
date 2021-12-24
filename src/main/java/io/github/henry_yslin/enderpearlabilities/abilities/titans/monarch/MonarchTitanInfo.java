package io.github.henry_yslin.enderpearlabilities.abilities.titans.monarch;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henry_yslin.enderpearlabilities.abilities.titans.TitanInfo;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class MonarchTitanInfo extends TitanInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 0);
        config.addDefault("cooldown", 300 * 20);
        config.addDefault("titan-ability-charge-up", 2 * 20);
        config.addDefault("titan-ability-cooldown", 10 * 20);
    }

    @Override
    public String getCodeName() {
        return "monarch-titan";
    }

    @Override
    public String getName() {
        return "Monarch Titan";
    }

    public String getTitanAbilityDescription() {
        return "Fire an electric blast that slows entity and regenerates health.";
    }

    public String getTitanAbilityName() {
        return "Energy Siphon";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    @Override
    public MonarchTitanAbility createInstance(String ownerName) {
        return new MonarchTitanAbility(plugin, this, ownerName);
    }

    public MonarchTitanInfo(Plugin plugin) {
        super(plugin);
    }
}
