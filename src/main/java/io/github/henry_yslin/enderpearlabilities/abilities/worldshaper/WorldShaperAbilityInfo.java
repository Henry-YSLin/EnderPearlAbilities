package io.github.henry_yslin.enderpearlabilities.abilities.worldshaper;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class WorldShaperAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 0);
        config.addDefault("cooldown", 20);
    }

    @Override
    public String getCodeName() {
        return "world-shaper";
    }

    @Override
    public String getName() {
        return "World Shaper";
    }

    @Override
    public String getOrigin() {
        return "Create Mod";
    }

    @Override
    public String getDescription() {
        return "Fires a projectile which explodes on impact, instantly mining a 3x3 area of blocks using the tool held in main hand.";
    }

    @Override
    public String getUsage() {
        return "Right click to fire. The tool held in main hand will be used to mine a 3x3 area where the projectile lands. Blocks inside this blast area will not be mined if the tool in main hand cannot produce block drops from those blocks. Tool durability is twice as efficient as manual mining and will not completely deplete while using this ability. There is a high chance for the ender pearl to drop as item after the blast.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.OffHand;
    }

    @Override
    public WorldShaperAbility createInstance(String ownerName) {
        return new WorldShaperAbility(plugin, this, ownerName);
    }

    public WorldShaperAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
