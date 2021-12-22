package io.github.henry_yslin.enderpearlabilities.abilities.fraggrenade;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class FragGrenadeAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 60);
        config.addDefault("cooldown", 400);
    }

    @Override
    public String getCodeName() {
        return "frag-grenade";
    }

    @Override
    public String getName() {
        return "Frag Grenade";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends";
    }

    @Override
    public String getDescription() {
        return "Throw frag grenades with accurate guides.";
    }

    @Override
    public String getUsage() {
        return "Hold an ender pearl to see the guide. Sneak to zoom in. Right click to throw.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.OffHand;
    }

    @Override
    public FragGrenadeAbility createInstance(String ownerName) {
        return new FragGrenadeAbility(plugin, this, ownerName);
    }

    public FragGrenadeAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
