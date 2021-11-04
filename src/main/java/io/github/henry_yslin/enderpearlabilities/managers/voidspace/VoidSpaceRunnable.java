package io.github.henry_yslin.enderpearlabilities.managers.voidspace;

import io.github.henry_yslin.enderpearlabilities.managers.ManagerRunnable;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class VoidSpaceRunnable extends ManagerRunnable {
    private final Player player;

    public VoidSpaceRunnable(Player player) {
        super();
        this.player = player;
    }

    @Override
    protected void start() {
        super.start();
    }

    @Override
    protected void tick() {
        if (!VoidSpaceManager.getInstance().isInVoid(player)) {
            cancel();
            return;
        }
        player.setRemainingAir(player.getMaximumAir());
        player.setFireTicks(0);
        player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation(), 10, 0.5, 1, 0.5, 0.02, null, true);
    }

    @Override
    protected void end() {
        super.end();
    }
}
