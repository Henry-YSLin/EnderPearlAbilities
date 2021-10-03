package io.github.henryyslin.enderpearlabilities.mirage;

import net.citizensnpcs.api.ai.EntityTarget;
import net.citizensnpcs.api.ai.flocking.Flocker;
import net.citizensnpcs.api.ai.flocking.RadiusNPCFlock;
import net.citizensnpcs.api.ai.flocking.SeparationBehavior;
import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;

@TraitName("cloneTrait")
public class CloneTrait extends Trait {
    @Persist("active")
    private boolean enabled = false;
    private Flocker flock;
    @Persist
    private UUID followingUUID;
    private Player player;
    @Persist
    private boolean protect;

    public CloneTrait() {
        super("cloneTrait");
    }

    public Player getFollowingPlayer() {
        return player;
    }

    /**
     * Returns whether the trait is actively following a {@link Player}.
     */
    public boolean isActive() {
        return enabled && npc.isSpawned() && player != null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void onDespawn() {
        flock = null;
    }

    @EventHandler
    private void onEntityDamage(EntityDamageByEntityEvent event) {
        if (isActive() && protect && event.getEntity().getName().equals(player.getName())) {
            Entity damager = event.getDamager();
            if (damager instanceof Projectile projectile) {
                if (projectile.getShooter() instanceof Entity) {
                    damager = (Entity) projectile.getShooter();
                }
            }
            if (damager.getName().equals(player.getName())) return;
            EntityTarget npcTarget = npc.getNavigator().getEntityTarget();
            if (npcTarget != null && npcTarget.isAggressive() && npcTarget.getTarget().isValid()) return;
            if (!(damager instanceof LivingEntity)) return;
            npc.getNavigator().setTarget(damager, true);
        }
    }

    @EventHandler
    public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        if (!isActive() || !protect) return;
        LivingEntity target = event.getTarget();
        Entity entity = event.getEntity();
        if (target == null) return;
        if (!(target.getName().equals(player.getName()))) return;
        EntityTarget npcTarget = npc.getNavigator().getEntityTarget();
        if (npcTarget != null && npcTarget.isAggressive() && npcTarget.getTarget().isValid()) return;
        if (!(entity instanceof LivingEntity)) return;
        npc.getNavigator().setTarget(entity, true);
    }

    @Override
    public void onSpawn() {
        flock = new Flocker(npc, new RadiusNPCFlock(4, 0), new SeparationBehavior(1));
        npc.getNavigator().getDefaultParameters().useNewPathfinder(true).speedModifier(1.5f).range(10).updatePathRate(20).stuckAction((npc, navigator) -> {
            npc.teleport(navigator.getTargetAsLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            return true;
        });
    }

    @Override
    public void run() {
        if (player == null || !player.isValid()) {
            if (followingUUID == null)
                return;
            player = Bukkit.getPlayer(followingUUID);
            if (player == null) {
                return;
            }
        }
        if (!isActive()) {
            return;
        }
        if (!npc.getEntity().getWorld().equals(player.getWorld())) {
            npc.teleport(player.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        }
        if (!npc.getNavigator().isNavigating()) {
            npc.getNavigator().setTarget(player, false);
        } else {
            flock.run();
        }
    }

    /**
     * Toggles and/or sets the {@link OfflinePlayer} to follow and whether to protect them (similar to wolves in
     * Minecraft, attack whoever attacks the player).
     *
     * Will toggle if the {@link OfflinePlayer} is the player currently being followed.
     *
     * @param player
     *            the player to follow
     * @param protect
     *            whether to protect the player
     * @return whether the trait is enabled
     */
    public boolean toggle(OfflinePlayer player, boolean protect) {
        this.protect = protect;
        if (player.getUniqueId().equals(this.followingUUID) || this.followingUUID == null) {
            this.enabled = !enabled;
        }
        this.followingUUID = player.getUniqueId();
        if (npc.getNavigator().isNavigating() && this.player != null && npc.getNavigator().getEntityTarget() != null
                && this.player == npc.getNavigator().getEntityTarget().getTarget()) {
            npc.getNavigator().cancelNavigation();
        }
        this.player = null;
        return this.enabled;
    }
}