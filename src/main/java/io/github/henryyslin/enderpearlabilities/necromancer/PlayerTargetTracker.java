package io.github.henryyslin.enderpearlabilities.necromancer;

import io.github.henryyslin.enderpearlabilities.utils.PlayerUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerTargetTracker extends BukkitRunnable {
    Player player;
    List<Skeleton> skeletons;
    AtomicReference<LivingEntity> playerTarget;

    public PlayerTargetTracker(Player player, List<Skeleton> skeletons, AtomicReference<LivingEntity> playerTarget) {
        this.player = player;
        this.skeletons = skeletons;
        this.playerTarget = playerTarget;
    }

    @Override
    public void run() {
        skeletons.removeIf(skeleton -> !skeleton.isValid());
        if (skeletons.isEmpty()) return;
        playerTarget.set(PlayerUtils.getPlayerTargetLivingEntity(player));
    }
}
