package io.github.henry_yslin.enderpearlabilities.abilities.necromancer;

import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;

import java.util.concurrent.atomic.AtomicReference;

public class SlaveSpawning extends AbilityRunnable {

    final Player player;
    final Skeleton skeleton;
    final AtomicReference<LivingEntity> playerTarget;
    Location particleLocation;

    public SlaveSpawning(Player player, Skeleton skeleton, AtomicReference<LivingEntity> playerTarget) {
        this.player = player;
        this.skeleton = skeleton;
        this.playerTarget = playerTarget;
    }

    @Override
    protected void start() {
        this.particleLocation = skeleton.getLocation().add(0, 1, 0);
    }

    @Override
    protected void tick() {
        if (!skeleton.isValid()) {
            cancel();
            return;
        }
        skeleton.teleport(skeleton.getLocation().add(0, 0.1, 0));
        skeleton.getWorld().spawnParticle(Particle.SQUID_INK, particleLocation, 2, 0.5, 1, 0.5, 0.02);
    }

    @Override
    protected void end() {
        if (!skeleton.isValid()) return;
        skeleton.setAI(true);
        if (this.hasCompleted())
            new SlaveAI(player, skeleton, playerTarget).runTaskTimer(executor, 0, 10);
        else {
            skeleton.setCustomName(null);
            skeleton.setCustomNameVisible(false);
            skeleton.removeMetadata("ability", executor.plugin);
            skeleton.setAI(true);
        }
    }
}
