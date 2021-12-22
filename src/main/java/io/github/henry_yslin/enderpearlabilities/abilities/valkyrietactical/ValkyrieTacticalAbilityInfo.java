package io.github.henry_yslin.enderpearlabilities.abilities.valkyrietactical;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class ValkyrieTacticalAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 15);
        config.addDefault("cooldown", 20 * 20);
    }

    @Override
    public String getCodeName() {
        return "valkyrie-tactical";
    }

    @Override
    public String getName() {
        return "Missile Swarm";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Valkyrie";
    }

    @Override
    public String getDescription() {
        return "Fire a swarm of missiles that damage and slow entities.\nPassive ability: Double-tap and hold the jump key to engage jetpack. Jetpack is disabled while you are falling or when elytra are equipped.";
    }

    @Override
    public String getUsage() {
        return "Right click to fire homing missiles towards your crosshair location. Entities hit by missiles will be slowed for a brief moment.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.OffHand;
    }

    @Override
    public ValkyrieTacticalAbility createInstance(String ownerName) {
        return new ValkyrieTacticalAbility(plugin, this, ownerName);
    }

    public ValkyrieTacticalAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
