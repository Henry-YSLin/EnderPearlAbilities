package io.github.henry_yslin.enderpearlabilities.abilities.frag;

import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.utils.WorldUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class FragPredictionRunnable extends AbilityRunnable {

    final Player player;

    public FragPredictionRunnable(Player player) {
        this.player = player;
    }

    private boolean shouldActivate(Player player) {
        if (executor.cooldown.getCoolingDown()) return false;
        return player.getInventory().getItemInOffHand().getType() == Material.ENDER_PEARL;
    }

    private Location getFirePosition(Player player) {
        return player.getEyeLocation().add(new Vector(0, 1, 0).crossProduct(player.getEyeLocation().getDirection()).multiply(0.4));
    }

    @Override
    public void tick() {
        if (!shouldActivate(player)) return;

        Vector velocity = player.getLocation().getDirection().normalize().multiply(FragAbility.PROJECTILE_SPEED);
        Location location = getFirePosition(player);
        Location lastLocation = location.clone();

        for (int i = 0; i < executor.getInfo().duration; i++) {
            location.add(velocity);
            velocity.multiply(0.98);
            velocity.add(new Vector(0, -0.1, 0));
            velocity.multiply(0.9);
            velocity.add(new Vector(0, -0.1, 0));
            WorldUtils.spawnPlayerParticleLine(player, lastLocation, location, Particle.ELECTRIC_SPARK, 2);
            lastLocation = location.clone();
        }
    }
}

