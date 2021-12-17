package io.github.henry_yslin.enderpearlabilities.managers.shield;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.managers.Manager;
import io.github.henry_yslin.enderpearlabilities.utils.MathUtils;
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

@Instantiable
public class ShieldManager extends Manager {

    @Override
    public String getCodeName() {
        return "shield";
    }

    private static ShieldManager instance = null;

    public static ShieldManager getInstance() {
        return instance;
    }

    public ShieldManager(Plugin plugin) {
        super(plugin);

        if (instance != null)
            throw new RuntimeException("ShieldManager already exists!");
        instance = this;
    }

    @Override
    protected void readFromConfig(ConfigurationSection config) {
        // no configs yet
    }

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        // no configs yet
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

    private static final List<EntityDamageEvent.DamageCause> ALLOWED_CAUSES = List.of(
            EntityDamageEvent.DamageCause.ENTITY_ATTACK,
            EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK,
            EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
    );

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (ALLOWED_CAUSES.contains(event.getCause())) {
            if (!(event.getDamager() instanceof LivingEntity damager)) return;
            Location origin = damager.getEyeLocation();
            RayTraceResult result = event.getEntity().getBoundingBox().rayTrace(origin.toVector(), origin.getDirection(), 5);
            Vector hitPosition;
            if (result != null) {
                hitPosition = result.getHitPosition();
            } else {
                if (event.getEntity() instanceof LivingEntity victim)
                    hitPosition = victim.getEyeLocation().toVector();
                else
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

