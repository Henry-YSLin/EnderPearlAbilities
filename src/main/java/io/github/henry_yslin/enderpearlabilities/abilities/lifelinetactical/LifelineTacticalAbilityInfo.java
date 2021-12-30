package io.github.henry_yslin.enderpearlabilities.abilities.lifelinetactical;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class LifelineTacticalAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 30);
        config.addDefault("duration", 10 * 20);
        config.addDefault("cooldown", 30 * 20);
    }

    @Override
    public String getCodeName() {
        return "lifeline-tactical";
    }

    @Override
    public String getName() {
        return "D.O.C. Heal Drone";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Lifeline";
    }

    @Override
    public String getDescription() {
        return "The Drone Of Compassion (DOC) automatically heals those near it over time.";
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
    public LifelineTacticalAbility createInstance(String ownerName) {
        return new LifelineTacticalAbility(plugin, this, ownerName);
    }

    public LifelineTacticalAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
