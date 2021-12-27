package io.github.henry_yslin.enderpearlabilities.abilities.revenantultimate;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class RevenantUltimateAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 2 * 20);
        config.addDefault("duration", 60 * 20);
        config.addDefault("cooldown", 180 * 20);
    }

    @Override
    public String getCodeName() {
        return "revenant-ultimate";
    }

    @Override
    public String getName() {
        return "Death Totem";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Revenant";
    }

    @Override
    public String getDescription() {
        return "Drop a totem that protects the user from death. Instead of getting killed, you will return to the totem.";
    }

    @Override
    public String getUsage() {
        return "Right click with an ender pearl to place the death totem. When you die while the totem is active, you will return to the totem instead with half health and all potion effects removed.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    @Override
    public RevenantUltimateAbility createInstance(String ownerName) {
        return new RevenantUltimateAbility(plugin, this, ownerName);
    }

    public RevenantUltimateAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
