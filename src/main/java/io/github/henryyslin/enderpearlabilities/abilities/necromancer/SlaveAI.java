package io.github.henryyslin.enderpearlabilities.abilities.necromancer;

import io.github.henryyslin.enderpearlabilities.utils.AbilityRunnable;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;

import java.util.concurrent.atomic.AtomicReference;

public class SlaveAI extends AbilityRunnable {
    final Player player;
    final Skeleton skeleton;
    final AtomicReference<LivingEntity> playerTarget;

    public SlaveAI(Player player, Skeleton skeleton, AtomicReference<LivingEntity> playerTarget) {
        this.player = player;
        this.skeleton = skeleton;
        this.playerTarget = playerTarget;
    }

    @Override
    public void tick() {
        if (!skeleton.isValid()) {
            cancel();
            return;
        }

        LivingEntity currentTarget = playerTarget.get();
        if (currentTarget != null) skeleton.setTarget(currentTarget);
        LivingEntity prevTarget = skeleton.getTarget();
        if (prevTarget != null) {
            if (prevTarget == player) skeleton.setTarget(null);
            else if (!prevTarget.isValid()) skeleton.setTarget(null);
        }

        skeleton.getWorld().spawnParticle(Particle.SOUL, skeleton.getLocation(), 5, 0.5, 0.5, 0.5, 0.02);
        LivingEntity skeletonTarget = skeleton.getTarget();
        if (skeletonTarget != null)
            skeleton.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, skeletonTarget.getLocation(), 10, 0.5, 1, 0.5, 0.05);
    }

    @Override
    protected void end() {
        if (skeleton.isValid()) {
            skeleton.setCustomName(null);
            skeleton.setCustomNameVisible(false);
            skeleton.removeMetadata("ability", ability.plugin);
            skeleton.setAI(true);
        }
    }
}
