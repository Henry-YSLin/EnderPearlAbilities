package io.github.henryyslin.enderpearlabilities.abilities.horizon;

import io.github.henryyslin.enderpearlabilities.utils.AbilityRunnable;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class AirControlRunnable extends AbilityRunnable {
    final Player player;
    Vector lastVelocity = null;

    public AirControlRunnable(Player player) {
        this.player = player;
    }

    @Override
    public void tick() {
        if (lastVelocity == null || player.isOnGround()) {
            lastVelocity = player.getVelocity();
            return;
        }
        Vector horizontalAccel = player.getVelocity().setY(0).subtract(lastVelocity.clone().setY(0));
        horizontalAccel.multiply(2);
        player.setVelocity(player.getVelocity().add(horizontalAccel));
        lastVelocity = player.getVelocity();
    }
}
