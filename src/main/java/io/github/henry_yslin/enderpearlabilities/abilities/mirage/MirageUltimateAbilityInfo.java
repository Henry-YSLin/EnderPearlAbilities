package io.github.henry_yslin.enderpearlabilities.abilities.mirage;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class MirageUltimateAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 40);
        config.addDefault("duration", 1000);
        config.addDefault("cooldown", 1000);
    }

    @Override
    public String getCodeName() {
        return "mirage-ultimate";
    }

    @Override
    public String getName() {
        return "Life of the Party";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Mirage";
    }

    @Override
    public String getDescription() {
        return "Deploy a team of decoys to distract enemies and protect the player.";
    }

    @Override
    public String getUsage() {
        return "Right click to summon decoys. Decoys last for a set amount of time and use tools on their hands with reduced efficiency. They wear the same armor and hold the same item in their main hand as the player. Decoys attack entities that damage them or the player. They will follow the player when idling. Mobs that target the player may get distracted by decoys.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.OffHand;
    }

    @Override
    public MirageUltimateAbility createInstance(String ownerName) {
        return new MirageUltimateAbility(plugin, this, ownerName);
    }

    public MirageUltimateAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
