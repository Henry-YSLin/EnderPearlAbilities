package io.github.henry_yslin.enderpearlabilities.abilities.revenanttactical;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class RevenantTacticalAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 10 * 20);
        config.addDefault("cooldown", 25 * 20);
    }

    @Override
    public String getCodeName() {
        return "revenant-tactical";
    }

    @Override
    public String getName() {
        return "Silence";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Revenant";
    }

    @Override
    public String getDescription() {
        return "Throw a device that deals damage and disables entity abilities.\nPassive ability: You crouch walk faster and jump higher when crouched.";
    }

    @Override
    public String getUsage() {
        return "Right click to fire. The lingering cloud deals damage to entities and disables their abilities.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.OffHand;
    }

    @Override
    public RevenantTacticalAbility createInstance(String ownerName) {
        return new RevenantTacticalAbility(plugin, this, ownerName);
    }

    public RevenantTacticalAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
