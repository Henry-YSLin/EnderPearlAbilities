package io.github.henry_yslin.enderpearlabilities.abilities.titans.ronin;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.titans.TitanInfo;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class RoninTitanInfo extends TitanInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 0);
        config.addDefault("cooldown", 300 * 20);
        config.addDefault("titan-ability-charge-up", 5);
        config.addDefault("titan-ability-cooldown", 10 * 20);
    }

    @Override
    public String getCodeName() {
        return "ronin-titan";
    }

    @Override
    public String getName() {
        return "Ronin Titan";
    }

    public String getTitanAbilityDescription() {
        return "Creates an electric wave that damages and slows entities hit by it.";
    }

    public String getTitanAbilityName() {
        return "Arc Wave";
    }

    @Override
    public RoninTitanAbility createInstance(String ownerName) {
        return new RoninTitanAbility(plugin, this, ownerName);
    }

    public RoninTitanInfo(Plugin plugin) {
        super(plugin);
    }
}
