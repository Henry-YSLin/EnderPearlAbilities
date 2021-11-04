package io.github.henry_yslin.enderpearlabilities.managers.voidspace;

import io.github.henry_yslin.enderpearlabilities.managers.Manager;
import io.github.henry_yslin.enderpearlabilities.managers.interactionlock.InteractionLockManager;
import org.bukkit.configuration.ConfigurationSection;
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

public class VoidSpaceManager extends Manager {

    @Override
    public String getName() {
        return "void-space";
    }

    private static VoidSpaceManager instance = null;

    public static VoidSpaceManager getInstance() {
        return instance;
    }

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
    }

    public VoidSpaceManager(Plugin plugin, ConfigurationSection config) {
        super(plugin, config);

        if (config == null) return; // do not assign instance if config is null since this is a template

        if (instance != null)
            throw new RuntimeException("VoidSpaceManager already exists!");
        instance = this;
    }

    private List<Player> playersInVoid = Collections.synchronizedList(new ArrayList<>());

    public boolean isInVoid(Player player) {
        return playersInVoid.contains(player);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        for (int i = playersInVoid.size() - 1; playersInVoid.size() > 0; i = playersInVoid.size() - 1) {
            exitVoid(playersInVoid.get(i));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!isInVoid(player)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isInVoid(player)) return;
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
        player.setInvisible(true);
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

        new VoidSpaceRunnable(player).runTaskTimer(this, 0, 1);
        playersInVoid.add(player);
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
        player.setInvisible(false);
        player.setFireTicks(0);
        player.removePotionEffect(PotionEffectType.CONDUIT_POWER);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.removePotionEffect(PotionEffectType.SPEED);

        playersInVoid.remove(player);
    }
}

