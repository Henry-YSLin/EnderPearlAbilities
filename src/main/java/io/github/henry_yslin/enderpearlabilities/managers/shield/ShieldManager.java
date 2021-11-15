package io.github.henry_yslin.enderpearlabilities.managers.shield;

import io.github.henry_yslin.enderpearlabilities.managers.Manager;
import io.github.henry_yslin.enderpearlabilities.utils.MathUtils;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ShieldManager extends Manager {

    @Override
    public String getName() {
        return "shield";
    }

    private static ShieldManager instance = null;

    public static ShieldManager getInstance() {
        return instance;
    }

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        super.setConfigDefaults(config);
    }

    public ShieldManager(Plugin plugin, ConfigurationSection config) {
        super(plugin, config);

        if (config == null) return; // do not assign instance if config is null since this is a template

        if (instance != null)
            throw new RuntimeException("ShieldManager already exists!");
        instance = this;
    }

    private final List<Shield> shields = Collections.synchronizedList(new ArrayList<>());
    private ShieldRunnable shieldRunnable;

    public List<Shield> getShields() {
        return shields;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        (shieldRunnable = new ShieldRunnable()).runTaskTimer(this, 0, 1);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (shieldRunnable != null && !shieldRunnable.isCancelled())
            shieldRunnable.cancel();
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            if (!(event.getDamager() instanceof LivingEntity damager)) return;
            Location origin = damager.getEyeLocation();
            RayTraceResult result = damager.getWorld().rayTrace(origin, origin.getDirection(), 5, FluidCollisionMode.NEVER, true, 0, entity -> entity.equals(event.getEntity()));
            Vector hitPosition;
            if (result != null) {
                hitPosition = result.getHitPosition();
            } else {
                hitPosition = event.getEntity().getLocation().toVector();
            }
            for (Shield shield : shields) {
                Optional<Vector> intersect = MathUtils.lineRectangleIntersect(origin.toVector(), hitPosition, shield.getBoundingBox(), shield.getNormal());
                intersect.ifPresent(vector -> {
                    Vector hitOffset = origin.toVector().subtract(vector);
                    shield.getBehavior().livingEntityWillMelee(shield, event, vector.toLocation(shield.getWorld()), hitOffset.dot(shield.getNormal()) > 0);
                });
            }
        }
    }

    public void addShield(Shield shield) {
        shields.add(shield);
    }

    public boolean removeShield(Shield shield) {
        return shields.remove(shield);
    }
}

