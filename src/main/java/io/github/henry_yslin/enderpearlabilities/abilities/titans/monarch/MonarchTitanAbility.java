package io.github.henry_yslin.enderpearlabilities.abilities.titans.monarch;

import io.github.henry_yslin.enderpearlabilities.abilities.titans.TitanAbility;
import io.github.henry_yslin.enderpearlabilities.utils.EntityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.WorldUtils;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class MonarchTitanAbility extends TitanAbility<MonarchTitanInfo> {

    static final int ENERGY_SIPHON_RANGE = 64;

    public MonarchTitanAbility(Plugin plugin, MonarchTitanInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }


    private Vector getHitPosition(RayTraceResult result) {
        if (result == null) {
            return player.getEyeLocation().toVector().add(player.getEyeLocation().getDirection().multiply(ENERGY_SIPHON_RANGE));
        } else {
            return result.getHitPosition();
        }
    }

    @Override
    public void onTitanAbilityChargeUp(IronGolem titan) {
        RayTraceResult result = player.getWorld().rayTrace(player.getEyeLocation(), player.getEyeLocation().getDirection(), ENERGY_SIPHON_RANGE, FluidCollisionMode.NEVER, true, 0,
                entity -> !entity.equals(player) && !entity.equals(titan) && entity instanceof LivingEntity);
        Vector hit = getHitPosition(result);
        WorldUtils.spawnParticleLine(titan.getEyeLocation(), hit.toLocation(titan.getWorld()), Particle.ELECTRIC_SPARK, 3, false);

    }

    @Override
    public void onTitanAbilityActivate(IronGolem titan) {
        RayTraceResult result = player.getWorld().rayTrace(player.getEyeLocation(), player.getEyeLocation().getDirection(), ENERGY_SIPHON_RANGE, FluidCollisionMode.NEVER, true, 0,
                entity -> !entity.equals(player) && !entity.equals(titan) && entity instanceof LivingEntity);
        Vector hit = getHitPosition(result);
        titan.getWorld().playSound(titan.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 2);
        boolean effective = false;
        if (result != null) {
            LivingEntity entity = (LivingEntity) result.getHitEntity();
            if (entity != null) {
                effective = true;
                entity.damage(4, player);
                entity.addPotionEffect(PotionEffectType.SLOW.createEffect(60, 1));
                titan.setHealth(Math.min(EntityUtils.getMaxHealth(titan), titan.getHealth() + 4));
            }
        }
        WorldUtils.spawnParticleLine(titan.getEyeLocation(), hit.toLocation(titan.getWorld()), effective ? Particle.DRAGON_BREATH : Particle.ELECTRIC_SPARK, 3, false);

    }
}
