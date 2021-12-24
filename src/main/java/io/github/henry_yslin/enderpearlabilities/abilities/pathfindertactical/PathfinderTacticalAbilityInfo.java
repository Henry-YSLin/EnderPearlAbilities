package io.github.henry_yslin.enderpearlabilities.abilities.pathfindertactical;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class PathfinderTacticalAbilityInfo extends AbilityInfo {

    protected int chargeUp;
    protected int duration;
    protected int minCooldown;
    protected int maxCooldown;

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 5 * 20);
        config.addDefault("min-cooldown", 5 * 20);
        config.addDefault("max-cooldown", 40 * 20);
    }

    @Override
    public String getCodeName() {
        return "pathfinder-tactical";
    }

    @Override
    public String getName() {
        return "Grappling Hook";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Pathfinder";
    }

    @Override
    public String getDescription() {
        return "Shoot a grappling hook to swing around, pull yourself up, or pull other entities close to you.\nPassive ability: Break your fall for a brief moment if the fall will be lethal.";
    }

    @Override
    public String getUsage() {
        return "Right click to throw a grapple. The grapple will anchor to where it hits. Look at the anchor while grappling to pull yourself towards the anchor. Look sideways to swing. Right click again to cancel the grapple.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.OffHand;
    }

    @Override
    public int getChargeUp() {
        return chargeUp;
    }

    @Override
    public int getDuration() {
        return duration;
    }

    @Override
    public int getCooldown() {
        return maxCooldown;
    }

    public int getMinCooldown() {
        return minCooldown;
    }

    public int getMaxCooldown() {
        return maxCooldown;
    }

    @Override
    protected void readFromConfig(ConfigurationSection config) {
        chargeUp = config.getInt("charge-up");
        duration = config.getInt("duration");
        minCooldown = config.getInt("min-cooldown");
        maxCooldown = config.getInt("max-cooldown");
    }

    @Override
    public PathfinderTacticalAbility createInstance(String ownerName) {
        return new PathfinderTacticalAbility(plugin, this, ownerName);
    }

    public PathfinderTacticalAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
