package io.github.henry_yslin.enderpearlabilities.managers.shield;

import org.bukkit.Location;
import org.bukkit.entity.Projectile;

public abstract class ShieldBehavior {
    public abstract int getTickInterval();

    public abstract void tick(Shield shield);

    public abstract void projectileWillHit(Shield shield, Projectile projectile, Location hitPosition, boolean backwardHit);
}
