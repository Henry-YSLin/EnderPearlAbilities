package io.github.henry_yslin.enderpearlabilities.abilities.bloodhoundultimate;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class BloodhoundUltimateAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 10);
        config.addDefault("duration", 30 * 20);
        config.addDefault("cooldown", 160 * 20);
    }

    @Override
    public String getCodeName() {
        return "bloodhound-ultimate";
    }

    @Override
    public String getName() {
        return "Beast of the Hunt";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Bloodhound";
    }

    @Override
    public String getDescription() {
        return "Transform into the ultimate hunter. Enhances your senses, allowing you to see in the dark and move faster. Killing entities extends duration.";
    }

    @Override
    public String getUsage() {
        return "Right click to activate. If you have the bloodhound tactical ability, you can use it more often. Killing entities extends duration up to the original duration.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    @Override
    public BloodhoundUltimateAbility createInstance(String ownerName) {
        return new BloodhoundUltimateAbility(plugin, this, ownerName);
    }

    public BloodhoundUltimateAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
