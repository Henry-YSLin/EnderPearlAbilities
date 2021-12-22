package io.github.henry_yslin.enderpearlabilities.abilities.titanfall;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class TitanfallAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 0);
        config.addDefault("cooldown", 300 * 20);
    }

    @Override
    public String getCodeName() {
        return "titanfall";
    }

    @Override
    public String getName() {
        return "Titanfall";
    }

    @Override
    public String getOrigin() {
        return "Titanfall";
    }

    @Override
    public String getDescription() {
        return "Deploy and pilot a titan for combat.\nTitan ability: Fire an electric blast that slows entity and regenerates health.\nPassive ability: dealing damage reduces titan cooldown.";
    }

    @Override
    public String getUsage() {
        return "Right click to summon a titan at your location. Use vehicle controls to mount and dismount the titan. While mounted, right click to switch between attack and move mode. Left click an entity in attack mode to lock target. Left click in move mode to jump. You are invincible while controlling the titan, but you will be ejected upwards when the titan is destroyed.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    @Override
    public TitanfallAbility createInstance(String ownerName) {
        return new TitanfallAbility(plugin, this, ownerName);
    }

    public TitanfallAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
