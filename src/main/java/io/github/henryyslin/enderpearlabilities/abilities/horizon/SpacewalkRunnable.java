package io.github.henryyslin.enderpearlabilities.abilities.horizon;

import io.github.henryyslin.enderpearlabilities.utils.AbilityRunnable;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class SpacewalkRunnable extends AbilityRunnable {

    final Player player;
    boolean activated = false;

    public SpacewalkRunnable(Player player) {
        this.player = player;
    }

    private static boolean shouldActivate(Player player) {
        if (player.getGameMode() != GameMode.ADVENTURE && player.getGameMode() != GameMode.SURVIVAL) return false;
        if (player.getVelocity().getY() >= 0) return false;
        if (player.getFallDistance() < 3) return false;

        RayTraceResult result = player.getWorld().rayTraceBlocks(player.getLocation(), new Vector(0, -1, 0), 2, FluidCollisionMode.NEVER);
        if (result == null) return false;
        return result.getHitBlock() != null;
    }

    @Override
    public void tick() {
        if (shouldActivate(player)) {
            if (activated) return;
            activated = true;
            player.setVelocity(player.getVelocity().setY(player.getVelocity().getY() / 5));
            player.setFallDistance(player.getFallDistance() - 5);
            player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 10, 0.1,0.1, 0.1, 0.02);
        } else {
            activated = false;
        }
    }
}
