package io.github.henry_yslin.enderpearlabilities.abilities.fraggrenade;

import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.utils.WorldUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class FragPredictionRunnable extends AbilityRunnable {

    final Player player;
    final FragGrenadeAbility ability;

    public FragPredictionRunnable(FragGrenadeAbility ability, Player player) {
        this.ability = ability; // TODO: read config from info
        this.player = player;
    }

    private boolean shouldActivate(Player player) {
        if (!ability.getCooldown().isAbilityUsable()) return false;
        return player.getInventory().getItemInOffHand().getType() == Material.ENDER_PEARL;
    }

    private Location getFirePosition(Player player) {
        return player.getEyeLocation().add(new Vector(0, 1, 0).crossProduct(player.getEyeLocation().getDirection()).multiply(0.4));
    }

    @Override
    public void tick() {
        if (!shouldActivate(player)) return;

        Vector velocity = player.getLocation().getDirection().normalize().multiply(FragGrenadeAbility.PROJECTILE_SPEED);
        Location location = getFirePosition(player);
        Location lastLocation = location.clone();

        for (int i = 0; i < ability.getInfo().getDuration() * 2 / 3; i++) {
            location.add(velocity);
            velocity.multiply(0.98);
            velocity.add(new Vector(0, -0.1, 0));
            velocity.multiply(0.9);
            velocity.add(new Vector(0, -0.1, 0));
            WorldUtils.spawnPlayerParticleLine(player, lastLocation, location, Particle.ELECTRIC_SPARK, 2);
            lastLocation = location.clone();
        }

        if (player.isSneaking())
            player.addPotionEffect(PotionEffectType.SLOW.createEffect(5, 20));
        else
            player.addPotionEffect(PotionEffectType.SLOW.createEffect(5, 1));
    }
}

