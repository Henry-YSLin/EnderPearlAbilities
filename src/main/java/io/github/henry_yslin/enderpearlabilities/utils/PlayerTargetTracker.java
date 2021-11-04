package io.github.henry_yslin.enderpearlabilities.utils;

import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Track the entity that is under the player's crosshair.
 */
public class PlayerTargetTracker extends AbilityRunnable {

    final Player player;
    final Supplier<Boolean> shouldTrack;
    final AtomicReference<LivingEntity> playerTarget;

    /**
     * Create a new instance of {@link PlayerTargetTracker}.
     *
     * @param player       The player to track.
     * @param shouldTrack  A condition to be checked on every tick. Tracking will be paused if this returns false.
     * @param playerTarget The reference to player's current target entity.
     */
    public PlayerTargetTracker(Player player, Supplier<Boolean> shouldTrack, AtomicReference<LivingEntity> playerTarget) {
        this.player = player;
        this.shouldTrack = shouldTrack;
        this.playerTarget = playerTarget;
    }

    @Override
    public void tick() {
        if (!shouldTrack.get()) return;
        playerTarget.set(PlayerUtils.getPlayerTargetLivingEntity(player));
    }
}
