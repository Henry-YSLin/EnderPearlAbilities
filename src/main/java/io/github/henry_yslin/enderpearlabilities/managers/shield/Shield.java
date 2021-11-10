package io.github.henry_yslin.enderpearlabilities.managers.shield;

import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Objects;

public class Shield {
    private World world;
    private BoundingBox boundingBox;
    private Vector normal;
    private ShieldBehavior behavior;

    public int nextTick;

    public Shield(World world, BoundingBox boundingBox, Vector normal, ShieldBehavior behavior) {
        this.world = world;
        this.boundingBox = boundingBox;
        this.normal = normal;
        this.behavior = behavior;
        this.nextTick = 0;
    }

    public World getWorld() {
        return world;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public Vector getNormal() {
        return normal;
    }

    public ShieldBehavior getBehavior() {
        return behavior;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Shield) obj;
        return Objects.equals(this.world, that.world) &&
                Objects.equals(this.boundingBox, that.boundingBox) &&
                Objects.equals(this.normal, that.normal) &&
                Objects.equals(this.behavior, that.behavior);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, boundingBox, normal, behavior);
    }

    @Override
    public String toString() {
        return "Shield[" +
                "world=" + world + ", " +
                "boundingBox=" + boundingBox + ", " +
                "normal=" + normal + ", " +
                "behavior=" + behavior + ']';
    }

}
