package io.github.henry_yslin.enderpearlabilities.abilities.horizontactical;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class HorizonTacticalAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 10 * 20);
        config.addDefault("cooldown", 20 * 20);
    }

    @Override
    public String getCodeName() {
        return "horizon-tactical";
    }

    @Override
    public String getName() {
        return "Gravity Lift";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Horizon";
    }

    @Override
    public String getDescription() {
        return "Reverses the flow of gravity, lifting players upward and boosting them outward when they exit.\nPassive ability: Cushion your fall to reduce fall damage.";
    }

    @Override
    public String getUsage() {
        return "Right click to throw a projectile. A gravity lift will be created where it lands.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.OffHand;
    }

    @Override
    public HorizonTacticalAbility createInstance(String ownerName) {
        return new HorizonTacticalAbility(plugin, this, ownerName);
    }

    public HorizonTacticalAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
