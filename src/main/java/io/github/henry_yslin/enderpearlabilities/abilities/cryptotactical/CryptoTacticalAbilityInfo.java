package io.github.henry_yslin.enderpearlabilities.abilities.cryptotactical;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class CryptoTacticalAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 20);
        config.addDefault("duration", 0);
        config.addDefault("cooldown", 800);
    }

    @Override
    public String getCodeName() {
        return "crypto-tactical";
    }

    @Override
    public String getName() {
        return "Surveillance Drone";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Crypto";
    }

    @Override
    public String getDescription() {
        return "Deploys an aerial drone for various purposes. Cooldown is only active if the drone is destroyed or recalled.";
    }

    @Override
    public String getUsage() {
        return "Right click to deploy a new drone or enter an existing one. Sneak while right-clicking to recall a deployed drone. Length of cooldown depends on the drone's heath. Right click while in drone to exit drone view and leave the drone in place. Ramming the drone into entities will cause damage to both the drone and the entity. Driving the drone through walls will temporarily limit vision.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.OffHand;
    }

    @Override
    public CryptoTacticalAbility createInstance(String ownerName) {
        return new CryptoTacticalAbility(plugin, this, ownerName);
    }

    public CryptoTacticalAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}