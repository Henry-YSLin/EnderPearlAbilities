package io.github.henry_yslin.enderpearlabilities.abilities.timefreeze;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class TimeFreezeAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 100);
        config.addDefault("cooldown", 600);
    }

    @Override
    public String getCodeName() {
        return "time-freeze";
    }

    @Override
    public String getName() {
        return "Time Freeze";
    }

    @Override
    public String getOrigin() {
        return "Original";
    }

    @Override
    public String getDescription() {
        return "Locally freeze time for a short duration.";
    }

    @Override
    public String getUsage() {
        return "Right click with an ender pearl to activate the ability. Frozen entities are invulnerable. Players are not affected by the freeze.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    @Override
    public TimeFreezeAbility createInstance(String ownerName) {
        return new TimeFreezeAbility(plugin, this, ownerName);
    }

    public TimeFreezeAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
