package io.github.henry_yslin.enderpearlabilities.abilities.cryptoultimate;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class CryptoUltimateAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 60);
        config.addDefault("duration", 200);
        config.addDefault("cooldown", 1200);
    }

    @Override
    public String getCodeName() {
        return "crypto-ultimate";
    }

    @Override
    public String getName() {
        return "EMP";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Crypto";
    }

    @Override
    public String getDescription() {
        return "Charge up an EMP blast from your drone (if any). Deals damage, slows entities and blocks their actions.";
    }

    @Override
    public String getUsage() {
        return "If a drone ability is available, you must have an existing drone to use this ability. Left click while in drone view, or right click with an ender pearl in person to charge up the EMP. EMP blast affects all entities in radius, including yourself.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    @Override
    public CryptoUltimateAbility createInstance(String ownerName) {
        return new CryptoUltimateAbility(plugin, this, ownerName);
    }

    public CryptoUltimateAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
