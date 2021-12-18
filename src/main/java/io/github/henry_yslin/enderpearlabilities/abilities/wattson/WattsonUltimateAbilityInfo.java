package io.github.henry_yslin.enderpearlabilities.abilities.wattson;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class WattsonUltimateAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 20);
        config.addDefault("duration", 1000);
        config.addDefault("cooldown", 800);
    }

    @Override
    public String getCodeName() {
        return "wattson-ultimate";
    }

    @Override
    public String getName() {
        return "Interception Pylon";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Wattson";
    }

    @Override
    public String getDescription() {
        return "Place an electrified pylon that destroys incoming projectiles and repairs damaged armors.";
    }

    @Override
    public String getUsage() {
        return "Right click on a block to place down the pylon. It will then intercept all projectiles and primed explosives in range that are not fired by the player who owns the pylon. It will also regenerate armor durability for all players in range.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    @Override
    public WattsonUltimateAbility createInstance(String ownerName) {
        return new WattsonUltimateAbility(plugin, this, ownerName);
    }

    public WattsonUltimateAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
