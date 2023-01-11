package io.github.henry_yslin.enderpearlabilities.abilities.fuseultimate;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class FuseUltimateAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 17 * 20);
        config.addDefault("cooldown", 160 * 20);
    }

    @Override
    public String getCodeName() {
        return "fuse-ultimate";
    }

    @Override
    public String getName() {
        return "The Motherlode";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Fuse";
    }

    @Override
    public String getDescription() {
        return "Launch a bombardment that encircles a target in a wall of flame. Enclosed targets are revealed.";
    }

    @Override
    public String getUsage() {
        return "Sneak to zoom in. Right click to launch a bombardment along the predicted path as shown.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    @Override
    public FuseUltimateAbility createInstance(String ownerName) {
        return new FuseUltimateAbility(plugin, this, ownerName);
    }

    public FuseUltimateAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
