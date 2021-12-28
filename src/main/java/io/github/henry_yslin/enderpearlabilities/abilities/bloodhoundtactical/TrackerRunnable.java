package io.github.henry_yslin.enderpearlabilities.abilities.bloodhoundtactical;

import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

public class TrackerRunnable extends AbilityRunnable {

    final Player player;

    public TrackerRunnable(Player player) {
        this.player = player;
    }

    @Override
    public void tick() {
        List<Entity> entities = player.getWorld().getNearbyEntities(player.getLocation(), 30, 30, 30,
                entity -> entity instanceof LivingEntity livingEntity
                        && !entity.equals(player)
                        && livingEntity.isOnGround()
                        && !livingEntity.isInsideVehicle()
                        && !livingEntity.isGliding()
                        && !livingEntity.isInWater()
        ).stream().toList();
        int particleCount = 2;
        if (entities.size() > 50) {
            return;
        } else if (entities.size() > 20) {
            particleCount = 1;
        }
        for (Entity entity : entities) {
            player.spawnParticle(Particle.LANDING_HONEY, entity.getLocation().add(0, 0.1, 0), particleCount, 0.1, 0.1, 0.1, 0);
        }
    }
}
