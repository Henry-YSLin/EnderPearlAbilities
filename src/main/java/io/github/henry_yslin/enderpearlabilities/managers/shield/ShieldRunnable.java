package io.github.henry_yslin.enderpearlabilities.managers.shield;

import io.github.henry_yslin.enderpearlabilities.managers.ManagerRunnable;
import io.github.henry_yslin.enderpearlabilities.utils.MathUtils;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.Optional;

public class ShieldRunnable extends ManagerRunnable {
    static final int PROJECTILE_CHECK_RADIUS = 20;

    private ShieldManager manager;

    public ShieldRunnable() {
        super();
    }

    @Override
    protected void start() {
        super.start();
        manager = ShieldManager.getInstance();
    }

    @Override
    protected void tick() {
        super.tick();
        for (Shield shield : manager.getShields()) {
            shield.nextTick--;
            if (shield.nextTick <= 0) {
                shield.nextTick = shield.getBehavior().getTickInterval();
                shield.getBehavior().tick(shield);
            }

            for (Entity entity : shield.getWorld().getNearbyEntities(shield.getBoundingBox().clone().expand(PROJECTILE_CHECK_RADIUS))) {
                Optional<Vector> intersect = MathUtils.lineRectangleIntersect(entity.getLocation().toVector(), entity.getLocation().add(entity.getVelocity()).toVector(), shield.getBoundingBox(), shield.getNormal());
                intersect.ifPresent(vector -> {
                    Vector hitOffset = entity.getLocation().toVector().subtract(vector);
                    shield.getBehavior().entityWillHit(shield, entity, vector.toLocation(shield.getWorld()), hitOffset.dot(shield.getNormal()) > 0);
                });
            }
        }
    }

    @Override
    protected void end() {
        super.end();
    }
}
