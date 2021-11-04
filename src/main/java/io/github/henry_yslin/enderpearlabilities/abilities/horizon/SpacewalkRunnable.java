package io.github.henry_yslin.enderpearlabilities.abilities.horizon;

import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
        if (player.getVelocity().getY() >= 0) return false;
        if (player.getFallDistance() < 3) return false;

        RayTraceResult result = player.getWorld().rayTraceBlocks(player.getLocation(), new Vector(0, -1, 0), 2, FluidCollisionMode.NEVER, true);
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
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR, 0.5f, 0);
            player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 10, 0.1, 0.1, 0.1, 0.02);
        } else {
            activated = false;
        }
    }
}
