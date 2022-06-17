package io.github.henry_yslin.enderpearlabilities.abilities.wraithultimate;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class WraithUltimateAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 20);
        config.addDefault("duration", 60 * 20);
        config.addDefault("cooldown", 120 * 20);
    }

    @Override
    public String getCodeName() {
        return "wraith-ultimate";
    }

    @Override
    public String getName() {
        return "Dimensional Rift";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Wraith";
    }

    @Override
    public String getDescription() {
        return "Link two locations with portals, allowing anyone to use them.";
    }

    @Override
    public String getUsage() {
        return "Right click with an ender pearl to set the first portal location. Go to the second location and right click again to set the second portal. You have extra buffs while setting portals. Portals have a limited distance and travelling to another dimension consumes extra portal distance.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    @Override
    public WraithUltimateAbility createInstance(String ownerName) {
        return new WraithUltimateAbility(plugin, this, ownerName);
    }

    public WraithUltimateAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
