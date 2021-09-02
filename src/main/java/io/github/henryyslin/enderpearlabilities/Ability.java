package io.github.henryyslin.enderpearlabilities;

import org.bukkit.event.Listener;

public interface Ability extends Listener {
    String getName();
    String getOrigin();
    String getConfigName();
    String getDescription();
    ActivationHand getActivation();
    int getChargeUp();
    int getDuration();
    int getCooldown();
}
