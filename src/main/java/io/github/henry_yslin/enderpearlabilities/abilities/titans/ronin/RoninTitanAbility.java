package io.github.henry_yslin.enderpearlabilities.abilities.titans.ronin;

import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.titans.TitanAbility;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class RoninTitanAbility extends TitanAbility<RoninTitanInfo> {

    static final int ARC_WAVE_LIFETIME = 10;
    static final int ARC_WAVE_SPEED = 2;
    static final int ARC_WAVE_EXTRA_GRAVITY = 1;

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
            IronGolem arcWave;
            Vector velocity;
            Location lastLocation = null;

            @Override
            protected void start() {
                velocity = player.getLocation().getDirection().setY(0).normalize().multiply(ARC_WAVE_SPEED);
                arcWave = titan.getWorld().spawn(titan.getLocation(), IronGolem.class, false, entity -> {
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
                if (!arcWave.isValid()) {
                    cancel();
                    return;
                }
                if (lastLocation != null) {
                    if (arcWave.getLocation().distanceSquared(lastLocation) < 0.001) {
                        cancel();
                        return;
                    }
                }
                lastLocation = arcWave.getLocation();
                for (Entity entity : arcWave.getWorld().getNearbyEntities(arcWave.getBoundingBox(),
                        entity -> entity instanceof LivingEntity && !entity.equals(titan) && !entity.equals(player))) {
                    LivingEntity livingEntity = (LivingEntity) entity;
                    livingEntity.damage(4, player);
                    livingEntity.addPotionEffect(PotionEffectType.SLOW.createEffect(60, 2));
                    livingEntity.addPotionEffect(PotionEffectType.BLINDNESS.createEffect(20, 1));
                }
                arcWave.setVelocity(velocity.setY(arcWave.getVelocity().getY() - ARC_WAVE_EXTRA_GRAVITY));
                Location center = arcWave.getLocation().clone().add(0, 1.5, 0);
                arcWave.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 20, 0.5, 0.7, 0.5, 0.01);
                arcWave.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, arcWave.getLocation(), 10, 0.5, 0.2, 0.5, 0.01);
            }

            @Override
            protected void end() {
                arcWave.remove();
            }
        }.runTaskRepeated(this, 0, 1, ARC_WAVE_LIFETIME);
    }
}
