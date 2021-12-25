package io.github.henry_yslin.enderpearlabilities.abilities.titans.ronin;

import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.titans.TitanAbility;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class RoninTitanAbility extends TitanAbility<RoninTitanInfo> {

    static final int LIFETIME = 10;
    static final int SPEED = 2;
    static final int EXTRA_GRAVITY = 2;
    static final double ANGLE_TOLERANCE = 45.0 / 180 * Math.PI;

    public RoninTitanAbility(Plugin plugin, RoninTitanInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    @Override
    public void onTitanAbilityChargeUp(IronGolem titan) {
        // nothing to do
    }

    @Override
    public void onTitanAbilityActivate(IronGolem titan) {
        new AbilityRunnable() {
            Mob arcWaveLarge;
            Mob arcWaveSmall;
            Vector velocity;
            Location lastLocation = null;

            @Override
            protected void start() {
                velocity = player.getLocation().getDirection().setY(0).normalize().multiply(SPEED);
                arcWaveLarge = titan.getWorld().spawn(titan.getLocation(), IronGolem.class, false, entity -> {
                    entity.setInvulnerable(true);
                    entity.setInvisible(true);
                    entity.setAware(false);
                    entity.setSilent(true);
                    entity.setCollidable(false);
                    entity.setVelocity(velocity);
                });
                arcWaveSmall = titan.getWorld().spawn(titan.getLocation(), Spider.class, false, entity -> {
                    entity.setInvulnerable(true);
                    entity.setInvisible(true);
                    entity.setAware(false);
                    entity.setSilent(true);
                    entity.setCollidable(false);
                    entity.setVelocity(velocity);
                });
            }

            @Override
            protected void tick() {
                if (!arcWaveSmall.isValid() || !arcWaveLarge.isValid()) {
                    cancel();
                    return;
                }
                if (lastLocation != null) {
                    Vector largeOffset = arcWaveLarge.getLocation().subtract(lastLocation).toVector().setY(0);
                    Vector smallOffset = arcWaveSmall.getLocation().subtract(lastLocation).toVector().setY(0);
                    Vector largeHorizontalOffset = largeOffset.clone().setY(0);
                    Vector smallHorizontalOffset = smallOffset.clone().setY(0);
                    double largeOffsetAngle = largeHorizontalOffset.angle(velocity.setY(0));
                    double smallOffsetAngle = smallHorizontalOffset.angle(velocity);
                    if (Math.min(largeOffsetAngle, smallOffsetAngle) > ANGLE_TOLERANCE) {
                        cancel();
                        return;
                    }
                    if (largeOffsetAngle > ANGLE_TOLERANCE) {
                        lastLocation = arcWaveSmall.getLocation();
                    } else if (smallOffsetAngle > ANGLE_TOLERANCE) {
                        lastLocation = arcWaveLarge.getLocation();
                    } else {
                        if (largeOffset.lengthSquared() >= smallOffset.lengthSquared()) {
                            lastLocation = arcWaveLarge.getLocation();
                        } else {
                            lastLocation = arcWaveSmall.getLocation();
                        }
                    }
                } else {
                    lastLocation = arcWaveSmall.getLocation();
                }

                for (Entity entity : arcWaveSmall.getWorld().getNearbyEntities(arcWaveLarge.getBoundingBox(),
                        entity -> entity instanceof LivingEntity
                                && !entity.equals(titan)
                                && !entity.equals(player)
                                && !entity.equals(arcWaveSmall)
                                && !entity.equals(arcWaveLarge))) {
                    LivingEntity livingEntity = (LivingEntity) entity;
                    livingEntity.damage(4, player);
                    livingEntity.addPotionEffect(PotionEffectType.SLOW.createEffect(60, 2));
                    livingEntity.addPotionEffect(PotionEffectType.BLINDNESS.createEffect(20, 1));
                }

                arcWaveLarge.setVelocity(velocity.setY(arcWaveLarge.getVelocity().getY() - EXTRA_GRAVITY));
                arcWaveSmall.setVelocity(velocity);
                arcWaveLarge.teleport(lastLocation);
                arcWaveSmall.teleport(lastLocation);
                arcWaveSmall.getWorld().spawnParticle(Particle.DRAGON_BREATH, arcWaveSmall.getLocation(), 20, 0.5, 0.7, 0.5, 0.01);
                arcWaveSmall.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, arcWaveSmall.getLocation(), 10, 0.5, 0.2, 0.5, 0.01);
                arcWaveSmall.getWorld().playSound(arcWaveSmall.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 2);
            }

            @Override
            protected void end() {
                arcWaveLarge.remove();
                arcWaveSmall.remove();
            }
        }.runTaskRepeated(this, 0, 1, LIFETIME);
    }
}
