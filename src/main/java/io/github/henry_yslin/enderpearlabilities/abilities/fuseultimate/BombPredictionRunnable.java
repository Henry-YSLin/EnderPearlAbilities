package io.github.henry_yslin.enderpearlabilities.abilities.fuseultimate;

import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henry_yslin.enderpearlabilities.utils.ProjectileUtils;
import io.github.henry_yslin.enderpearlabilities.utils.WorldUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class BombPredictionRunnable extends AbilityRunnable {

    final Player player;
    final FuseUltimateAbility ability;

    public BombPredictionRunnable(FuseUltimateAbility ability, Player player) {
        this.ability = ability; // TODO: read config from info
        this.player = player;
    }

    private boolean shouldActivate(Player player) {
        if (!ability.getCooldown().isAbilityUsable()) return false;
        if (this.ability.getInfo().getActivation() == ActivationHand.MainHand)
            return player.getInventory().getItemInMainHand().getType() == Material.ENDER_PEARL;
        else
            return player.getInventory().getItemInOffHand().getType() == Material.ENDER_PEARL;
    }

    private Location getFirePosition(Player player) {
        return player.getEyeLocation().add(new Vector(0, 1, 0).crossProduct(player.getEyeLocation().getDirection()).multiply(-0.4));
    }

    @Override
    public void tick() {
        if (!shouldActivate(player)) return;

        if (player.isSneaking())
            player.addPotionEffect(PotionEffectType.SLOW.createEffect(5, 50));
        else
            player.addPotionEffect(PotionEffectType.SLOW.createEffect(5, 1));

        Location target = ability.getExplosionLocation(player);
        if (target == null) return;
        Vector velocity = ProjectileUtils.computeProjectileVelocity(
                player.getEyeLocation(),
                0.03,
                0.01,
                target,
                FuseUltimateAbility.PROJECTILE_SPEED,
                FuseUltimateAbility.PROJECTILE_LIFETIME
        );
        Location location = getFirePosition(player);
        Location lastLocation = location.clone();

        for (int i = 0; i < 100; i++) {
            location.add(velocity);
            velocity.multiply(0.98);
            velocity.add(new Vector(0, -0.1, 0));
            WorldUtils.spawnPlayerParticleLine(player, lastLocation, location, Particle.ELECTRIC_SPARK, 2);
            if (location.distanceSquared(target) < velocity.lengthSquared() + 1) break;
            lastLocation = location.clone();
        }
        player.spawnParticle(Particle.ELECTRIC_SPARK, target, 20, 1, 1, 1, 0.01);
        WorldUtils.spawnPlayerParticleLine(player, target, target.clone().add(0, -FuseUltimateAbility.EXPLOSION_HEIGHT, 0), Particle.ELECTRIC_SPARK, 2);
    }
}

