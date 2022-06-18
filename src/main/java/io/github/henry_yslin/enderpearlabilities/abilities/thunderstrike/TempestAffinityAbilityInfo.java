package io.github.henry_yslin.enderpearlabilities.abilities.thunderstrike;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class TempestAffinityAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 5 * 20);
        config.addDefault("duration", 30 * 20);
        config.addDefault("cooldown", 180 * 20);
    }

    @Override
    public String getCodeName() {
        return "tempest-affinity";
    }

    @Override
    public String getName() {
        return "Tempest Affinity";
    }

    @Override
    public String getOrigin() {
        return "Original";
    }

    @Override
    public String getDescription() {
        return "Summon a thunderstorm to wreak havoc on the world. Striking entities with lightning bolt extends thunderstorm duration.\nPassive ability: Gain speed boost when the weather is not clear.";
    }

    @Override
    public String getUsage() {
        return "Right click to activate. Striking entities with lightning bolts extends the duration of the thunderstorm up to the original duration.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    @Override
    public TempestAffinityAbility createInstance(String ownerName) {
        return new TempestAffinityAbility(plugin, this, ownerName);
    }

    public TempestAffinityAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
