package io.github.henry_yslin.enderpearlabilities.abilities.bangaloreultimate;

import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;

public class MissileAI extends AbilityRunnable {

    final Player player;
    final TNTPrimed tnt;

    public MissileAI(Player player, TNTPrimed tnt) {
        this.player = player;
        this.tnt = tnt;
    }

    @Override
    public void tick() {
        if (!tnt.isValid()) {
            cancel();
            return;
        }

        tnt.setVelocity(tnt.getVelocity().setX(0).setZ(0));

        // todo: trail not visible
        tnt.getWorld().spawnParticle(Particle.SMOKE_LARGE, tnt.getLocation(), 2, 0.1, 0.1, 0.1, 0, null, true);
    }

    @Override
    protected void end() {
        // todo: explode
    }
}
