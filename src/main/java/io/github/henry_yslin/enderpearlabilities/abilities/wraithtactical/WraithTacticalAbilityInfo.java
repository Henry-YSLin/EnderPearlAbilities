package io.github.henry_yslin.enderpearlabilities.abilities.wraithtactical;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class WraithTacticalAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 30);
        config.addDefault("duration", 7 * 20);
        config.addDefault("cooldown", 20 * 20);
    }

    @Override
    public String getCodeName() {
        return "wraith-tactical";
    }

    @Override
    public String getName() {
        return "Into The Void";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Wraith";
    }

    @Override
    public String getDescription() {
        return "Reposition quickly through the safety of void space, avoiding all damage and interactions.\nPassive ability: A voice warns you when danger approaches.";
    }

    @Override
    public String getUsage() {
        return "Right click with an ender pearl to activate the ability. Right click again to exit early. You may not interact with anything while the ability is active.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.OffHand;
    }

    @Override
    public WraithTacticalAbility createInstance(String ownerName) {
        return new WraithTacticalAbility(plugin, this, ownerName);
    }

    public WraithTacticalAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
