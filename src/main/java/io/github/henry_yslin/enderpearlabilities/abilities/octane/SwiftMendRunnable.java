package io.github.henry_yslin.enderpearlabilities.abilities.octane;

import io.github.henry_yslin.enderpearlabilities.utils.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.utils.EntityUtils;
import org.bukkit.entity.Player;

public class SwiftMendRunnable extends AbilityRunnable {

    final Player player;
    double maxHealth;

    public SwiftMendRunnable(Player player) {
        this.player = player;
        maxHealth = EntityUtils.getMaxHealth(player);
        if (maxHealth == 0) maxHealth = Double.POSITIVE_INFINITY;
    }

    @Override
    public void tick() {
        if (player.isValid())
            player.setHealth(Math.min(maxHealth, player.getHealth() + 0.2));
    }
}
