package io.github.henry_yslin.enderpearlabilities.managers.voidspace;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.managers.Manager;
import io.github.henry_yslin.enderpearlabilities.managers.interactionlock.InteractionLockManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Instantiable
public class VoidSpaceManager extends Manager {

    @Override
    public String getCodeName() {
        return "void-space";
    }

    private static VoidSpaceManager instance = null;

    public static VoidSpaceManager getInstance() {
        return instance;
    }

    public VoidSpaceManager(Plugin plugin) {
        super(plugin);

        if (instance != null)
            throw new RuntimeException("VoidSpaceManager already exists!");
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

    private final List<Player> playersInVoid = Collections.synchronizedList(new ArrayList<>());
    private final List<LivingEntity> entitiesInVoid = Collections.synchronizedList(new ArrayList<>());
    private VoidSpaceRunnable voidSpaceRunnable;

    public boolean isInVoid(Player player) {
        return playersInVoid.contains(player);
    }

    public boolean isInVoid(LivingEntity entity) {
        if (entity instanceof Player player) {
            return isInVoid(player);
        }
        return entitiesInVoid.contains(entity);
    }

    @Override
    public void onEnable() {
        super.onEnable();

        if (voidSpaceRunnable != null && !voidSpaceRunnable.isCancelled())
            voidSpaceRunnable.cancel();
        voidSpaceRunnable = new VoidSpaceRunnable(playersInVoid, entitiesInVoid);
        voidSpaceRunnable.runTaskTimer(this, 0, 1);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        for (int i = playersInVoid.size() - 1; playersInVoid.size() > 0; i = playersInVoid.size() - 1) {
            exitVoid(playersInVoid.get(i));
        }
        for (int i = entitiesInVoid.size() - 1; entitiesInVoid.size() > 0; i = entitiesInVoid.size() - 1) {
            exitVoid(entitiesInVoid.get(i));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity livingEntity)) return;
        if (!isInVoid(livingEntity)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!isInVoid(event.getEntity())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPickupArrow(PlayerPickupArrowEvent event) {
        if (!isInVoid(event.getPlayer())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!isInVoid(event.getPlayer())) return;
        event.setCancelled(true);
    }

    public void enterVoid(Player player) {
        if (isInVoid(player)) return;

        InteractionLockManager.getInstance().lockInteraction(player);
        player.setCollidable(false);
        player.setInvulnerable(true);
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (!isInVoid(onlinePlayer))
                onlinePlayer.hidePlayer(plugin, player);
        }
        for (Player voidPlayer : playersInVoid) {
            player.showPlayer(plugin, voidPlayer);
        }
        player.addPotionEffect(PotionEffectType.CONDUIT_POWER.createEffect(Integer.MAX_VALUE, 1));
        player.addPotionEffect(PotionEffectType.NIGHT_VISION.createEffect(Integer.MAX_VALUE, 1));
        player.addPotionEffect(PotionEffectType.SPEED.createEffect(Integer.MAX_VALUE, 1));

        playersInVoid.add(player);
    }

    public void enterVoid(LivingEntity entity) {
        if (isInVoid(entity)) return;
        if (entity instanceof Player player) {
            enterVoid(player);
            return;
        }

        entity.setCollidable(false);
        entity.setInvulnerable(true);
        entity.addPotionEffect(PotionEffectType.SPEED.createEffect(Integer.MAX_VALUE, 1));

        entitiesInVoid.add(entity);
    }

    public void exitVoid(Player player) {
        if (!isInVoid(player)) return;

        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            onlinePlayer.showPlayer(plugin, player);
        }
        for (Player voidPlayer : playersInVoid) {
            player.hidePlayer(plugin, voidPlayer);
        }
        InteractionLockManager.getInstance().unlockInteraction(player);
        player.setCollidable(true);
        player.setInvulnerable(false);
        player.setFireTicks(0);
        player.removePotionEffect(PotionEffectType.CONDUIT_POWER);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.removePotionEffect(PotionEffectType.SPEED);

        playersInVoid.remove(player);
    }

    public void exitVoid(LivingEntity entity) {
        if (!isInVoid(entity)) return;
        if (entity instanceof Player player) {
            exitVoid(player);
            return;
        }

        entity.setCollidable(true);
        entity.setInvulnerable(false);
        entity.setFireTicks(0);
        entity.removePotionEffect(PotionEffectType.SPEED);

        entitiesInVoid.remove(entity);
    }
}

