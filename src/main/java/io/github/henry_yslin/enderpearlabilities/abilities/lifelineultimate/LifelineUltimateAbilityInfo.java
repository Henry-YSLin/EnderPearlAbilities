package io.github.henry_yslin.enderpearlabilities.abilities.lifelineultimate;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class LifelineUltimateAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 0);
        config.addDefault("cooldown", 5 * 60 * 20);
    }

    @Override
    public String getCodeName() {
        return "lifeline-ultimate";
    }

    @Override
    public String getName() {
        return "Care Package";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Lifeline";
    }

    @Override
    public String getDescription() {
        return "Call in a drop pod full of high quality defensive gear.";
    }

    @Override
    public String getUsage() {
        return "Right click on a block to signal a drop pod to land there. The drop pod will contain food, enchanted books and armor pieces that are an upgrade to what you already have.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    @Override
    public LifelineUltimateAbility createInstance(String ownerName) {
        return new LifelineUltimateAbility(plugin, this, ownerName);
    }

    public LifelineUltimateAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
