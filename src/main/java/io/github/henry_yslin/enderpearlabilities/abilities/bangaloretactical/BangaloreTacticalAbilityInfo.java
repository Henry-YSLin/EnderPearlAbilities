package io.github.henry_yslin.enderpearlabilities.abilities.bangaloretactical;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class BangaloreTacticalAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 400);
        config.addDefault("cooldown", 1000);
    }

    @Override
    public String getCodeName() {
        return "bangalore-tactical";
    }

    @Override
    public String getName() {
        return "Smoke Launcher";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Bangalore";
    }

    @Override
    public String getDescription() {
        return "Fire a high-velocity smoke canister that explodes into a smoke wall on impact.\nPassive ability: Taking fire or damage while sprinting makes you move faster for a brief time.";
    }

    @Override
    public String getUsage() {
        return "Right click to fire a smoke canister. The smoke wall will deal small damage and protect players from mob targeting.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.OffHand;
    }

    @Override
    public BangaloreTacticalAbility createInstance(String ownerName) {
        return new BangaloreTacticalAbility(plugin, this, ownerName);
    }

    public BangaloreTacticalAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
