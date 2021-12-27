package io.github.henry_yslin.enderpearlabilities.abilities.seerultimate;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class SeerUltimateAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 20);
        config.addDefault("duration", 45 * 20);
        config.addDefault("cooldown", 120 * 20);
    }

    @Override
    public String getCodeName() {
        return "seer-ultimate";
    }

    @Override
    public String getName() {
        return "Exhibit";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Seer";
    }

    @Override
    public String getDescription() {
        return "Create a sphere of micro-drones that reveal the location of entities not sneaking.";
    }

    @Override
    public String getUsage() {
        return "Right click to throw a projectile. The sphere of micro-drones will be centered at where it lands. Entities not sneaking will be revealed.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    @Override
    public SeerUltimateAbility createInstance(String ownerName) {
        return new SeerUltimateAbility(plugin, this, ownerName);
    }

    public SeerUltimateAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
