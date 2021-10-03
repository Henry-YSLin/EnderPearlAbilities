package io.github.henryyslin.enderpearlabilities.utils;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class PlayerTargetTracker extends BukkitRunnable {
    Player player;
    Supplier<Boolean> shouldTrack;
    AtomicReference<LivingEntity> playerTarget;

    public PlayerTargetTracker(Player player, Supplier<Boolean> shouldTrack, AtomicReference<LivingEntity> playerTarget) {
        this.player = player;
        this.shouldTrack = shouldTrack;
        this.playerTarget = playerTarget;
    }

    @Override
    public void run() {
        if (!shouldTrack.get()) return;
        playerTarget.set(PlayerUtils.getPlayerTargetLivingEntity(player));
    }
}
