package io.github.henry_yslin.enderpearlabilities.managers.voidspace;

import io.github.henry_yslin.enderpearlabilities.managers.ManagerRunnable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

public class VoidSpaceRunnable extends ManagerRunnable {
    private final List<Player> playersInVoid;
    private final List<LivingEntity> entitiesInVoid;


    public VoidSpaceRunnable(List<Player> playersInVoid, List<LivingEntity> entitiesInVoid) {
        super();
        this.playersInVoid = playersInVoid;
        this.entitiesInVoid = entitiesInVoid;
    }

    @Override
    protected void start() {
        super.start();
    }

    @Override
    protected void tick() {
        synchronized (playersInVoid) {
            for (Player player : playersInVoid) {
                player.setRemainingAir(player.getMaximumAir());
                player.setFireTicks(0);
            }
        }
        synchronized (entitiesInVoid) {
            for (LivingEntity entity : entitiesInVoid) {
                entity.setRemainingAir(entity.getMaximumAir());
                entity.setFireTicks(0);
            }
        }
    }

    @Override
    protected void end() {
        super.end();
    }
}
