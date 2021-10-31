package io.github.henryyslin.enderpearlabilities.abilities.octane;

import io.github.henryyslin.enderpearlabilities.utils.AbilityRunnable;
import io.github.henryyslin.enderpearlabilities.utils.EntityUtils;
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
        player.setHealth(Math.min(maxHealth, player.getHealth() + 0.2));
    }
}
