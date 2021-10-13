package io.github.henryyslin.enderpearlabilities.abilities.pathfinder;

import io.github.henryyslin.enderpearlabilities.utils.AbilityRunnable;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class SlowFallRunnable extends AbilityRunnable {

    final Player player;
    boolean activated = false;
    Vector prevVelocity;
    long lastActivation = -1;
    static final int DURATION = 20;

    public SlowFallRunnable(Player player) {
        this.player = player;
    }

    private static boolean shouldActivate(Player player) {
        if (player.getVelocity().getY() >= 0) return false;
        if (player.getFallDistance() > player.getHealth() + 2) return true;
        RayTraceResult result = player.getWorld().rayTraceBlocks(player.getLocation(), new Vector(0, -1, 0), 50, FluidCollisionMode.ALWAYS);
        if (result == null) return true;
        if (result.getHitBlock() == null) return true;
        if (result.getHitBlock().getType() == Material.LAVA) return true;
        double totalFallDist = player.getLocation().getY() - result.getHitBlock().getLocation().getY() + player.getFallDistance();
        if (result.getHitBlock().getType() == Material.POINTED_DRIPSTONE) totalFallDist *= 2;
        return totalFallDist > player.getHealth() + 2;
    }

    @Override
    public void tick() {
        //noinspection deprecation
        if (player.isOnGround()) {
            activated = false;
            lastActivation = -1;
            prevVelocity = null;
            player.removePotionEffect(PotionEffectType.SLOW_FALLING);
            return;
        }
        if (lastActivation > 0) lastActivation -= 5;
        if (prevVelocity != null && lastActivation <= 0) {
            player.removePotionEffect(PotionEffectType.SLOW_FALLING);
            player.setVelocity(prevVelocity);
            prevVelocity = null;
            lastActivation = -1;
        }
        if (activated) return;
        if (shouldActivate(player)) {
            player.addPotionEffect(PotionEffectType.SLOW_FALLING.createEffect(DURATION * 10, 3));
            activated = true;
            lastActivation = DURATION;
            prevVelocity = player.getVelocity();
        }
    }
}
