package io.github.henry_yslin.enderpearlabilities.managers.shield;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public abstract class ShieldBehavior {
    public abstract int getTickInterval();

    public abstract void tick(Shield shield);

    public abstract void entityWillHit(Shield shield, Entity entity, Location hitPosition, boolean backwardHit);

    public abstract void livingEntityWillMelee(Shield shield, EntityDamageByEntityEvent event, Location hitPosition, boolean backwardHit);
}
