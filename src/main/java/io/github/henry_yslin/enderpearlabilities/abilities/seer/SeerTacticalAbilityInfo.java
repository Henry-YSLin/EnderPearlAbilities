package io.github.henry_yslin.enderpearlabilities.abilities.seer;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class SeerTacticalAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 30);
        config.addDefault("duration", 200);
        config.addDefault("cooldown", 500);
    }

    @Override
    public String getCodeName() {
        return "seer-tactical";
    }

    @Override
    public String getName() {
        return "Focus of Attention";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Seer";
    }

    @Override
    public String getDescription() {
        return "Summon micro-drones to emit a delayed blast that goes through walls blinding and revealing entities.\nPassive ability: Visualize the heartbeats of nearby entities when sneaking.";
    }

    @Override
    public String getUsage() {
        return "Right click to activate. Entities hit by the blast receive debuffs and remain revealed for the duration of the ability. If no entities are hit, the ability goes into cooldown early.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.OffHand;
    }

    @Override
    public SeerTacticalAbility createInstance(String ownerName) {
        return new SeerTacticalAbility(plugin, this, ownerName);
    }

    public SeerTacticalAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
