package io.github.henry_yslin.enderpearlabilities.abilities.horizon;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class HorizonUltimateAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 20);
        config.addDefault("duration", 200);
        config.addDefault("cooldown", 1000);
    }

    @Override
    public String getCodeName() {
        return "horizon-ultimate";
    }

    @Override
    public String getName() {
        return "Black Hole";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Horizon";
    }

    @Override
    public String getDescription() {
        return "Create an inescapable micro black hole that pulls all entities in towards it.\nPassive ability: Cushion your fall to reduce fall damage.";
    }

    @Override
    public String getUsage() {
        return "Right click to throw a projectile. A black hole will be created where it lands.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    @Override
    public Ability createInstance(String ownerName) {
        return new HorizonUltimateAbility(plugin, this, ownerName);
    }

    public HorizonUltimateAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
