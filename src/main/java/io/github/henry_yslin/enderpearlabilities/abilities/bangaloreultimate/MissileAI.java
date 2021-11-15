package io.github.henry_yslin.enderpearlabilities.abilities.bangaloreultimate;

import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.utils.WorldUtils;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class MissileAI extends AbilityRunnable {

    static final double EXPLOSION_RADIUS = 4;

    final Player player;
    final TNTPrimed tnt;
    boolean onGround = false;

    public MissileAI(Player player, TNTPrimed tnt) {
        this.player = player;
        this.tnt = tnt;
    }

    @Override
    public void tick() {
        if (!tnt.isValid()) {
            cancel();
            return;
        }

        tnt.setVelocity(tnt.getVelocity().setX(0).setZ(0));

        if (!onGround && tnt.isOnGround()) {
            tnt.getWorld().playSound(tnt.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_HURT, 1, 0);
            onGround = true;
        }

        WorldUtils.spawnParticleLine(tnt.getLocation(), tnt.getLocation().add(tnt.getVelocity()), Particle.SMOKE_LARGE, 1, true);
    }

    @Override
    protected void end() {
        Location center = tnt.getLocation().add(0, 0.5, 0);
        tnt.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, center, 1, 0, 0, 0, 0, null, true);
        tnt.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1, 0);
        for (Entity entity : tnt.getWorld().getNearbyEntities(center, EXPLOSION_RADIUS, EXPLOSION_RADIUS, EXPLOSION_RADIUS, entity -> entity instanceof LivingEntity)) {
            LivingEntity livingEntity = (LivingEntity) entity;
            Vector offset = livingEntity.getEyeLocation().toVector().subtract(center.toVector());
            RayTraceResult result = tnt.getWorld().rayTraceBlocks(center, offset, offset.length(), FluidCollisionMode.NEVER, true);
            if (result == null) {
                livingEntity.damage(8, player);
                livingEntity.addPotionEffect(PotionEffectType.SLOW.createEffect(200, 3));
                livingEntity.addPotionEffect(PotionEffectType.BLINDNESS.createEffect(100, 0));
                livingEntity.addPotionEffect(PotionEffectType.CONFUSION.createEffect(160, 1));
            }
        }
        tnt.remove();
    }
}
