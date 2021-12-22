package io.github.henry_yslin.enderpearlabilities.abilities.gibraltartactical;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class GibraltarTacticalAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 12 * 20);
        config.addDefault("cooldown", 30 * 20);
    }

    @Override
    public String getCodeName() {
        return "gibraltar-tactical";
    }

    @Override
    public String getName() {
        return "Dome of Protection";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Gibraltar";
    }

    @Override
    public String getDescription() {
        return "Blocks incoming and outgoing attacks.";
    }

    @Override
    public String getUsage() {
        return "Right click to throw a disc that projects a shield around it.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.OffHand;
    }

    @Override
    public GibraltarTacticalAbility createInstance(String ownerName) {
        return new GibraltarTacticalAbility(plugin, this, ownerName);
    }

    public GibraltarTacticalAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
