package io.github.henryyslin.enderpearlabilities.utils;

import io.github.henryyslin.enderpearlabilities.ActivationHand;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AbilityUtils {
    public static void chargeUpSequence(Plugin plugin, Player player, int chargeUp, NextFunction next) {
        new AdvancedRunnable() {
            BossBar bossbar;

            @Override
            protected synchronized void start() {
                player.setCooldown(Material.ENDER_PEARL, chargeUp);
                bossbar = Bukkit.createBossBar("Charging up", BarColor.WHITE, BarStyle.SOLID);
                bossbar.addPlayer(player);
            }

            @Override
            protected synchronized void tick() {
                bossbar.setProgress(count / (double) chargeUp);
                player.getWorld().spawnParticle(Particle.WHITE_ASH, player.getLocation(), 5, 0.5, 0.5, 0.5, 0.02);
            }

            @Override
            protected synchronized void end() {
                bossbar.removeAll();
                next.invoke();
            }
        }.runTaskRepeated(plugin, 0, 1, chargeUp);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean abilityShouldActivate(PlayerInteractEvent event, String ownerName, ActivationHand preferredHand) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack item = event.getItem();

        if (!player.getName().equals(ownerName)) return false;
        if (preferredHand == ActivationHand.MainHand) {
            if (player.getInventory().getItemInMainHand().getType() != Material.ENDER_PEARL) return false;
        } else {
            if (player.getInventory().getItemInOffHand().getType() != Material.ENDER_PEARL) return false;
        }
        if (item == null || item.getType() != Material.ENDER_PEARL) return false;
        if (player.getCooldown(Material.ENDER_PEARL) > 0) return false;
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }

    public static boolean consumeEnderPearl(Player player) {
        if (player.getGameMode() != GameMode.CREATIVE) {
            HashMap<Integer, ItemStack> remainingItems = player.getInventory().removeItem(new ItemStack(Material.ENDER_PEARL, 1));
            player.updateInventory();
            if (!remainingItems.isEmpty()) {
                ItemStack heldItem = player.getInventory().getItemInMainHand();
                if (heldItem.getType() == Material.ENDER_PEARL) {
                    heldItem.setAmount(heldItem.getAmount() - 1);
                    if (heldItem.getAmount() == 0) {
                        heldItem.setType(Material.AIR);
                    }
                    player.getInventory().setItemInMainHand(heldItem);
                } else {
                    heldItem = player.getInventory().getItemInOffHand();
                    if (heldItem.getType() == Material.ENDER_PEARL) {
                        heldItem.setAmount(heldItem.getAmount() - 1);
                        if (heldItem.getAmount() == 0) {
                            heldItem.setType(Material.AIR);
                        }
                        player.getInventory().setItemInOffHand(heldItem);
                    } else {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static Projectile relaunchEnderPearl(Plugin plugin, Player player, AtomicBoolean blockShoot, int projectileLifetime, double projectileSpeed) {
        return relaunchEnderPearl(plugin, player, blockShoot, projectileLifetime, projectileSpeed, false);
    }

    public static Projectile relaunchEnderPearl(Plugin plugin, Player player, AtomicBoolean blockShoot, int projectileLifetime, double projectileSpeed, boolean gravity) {
        if (blockShoot != null) {
            if (blockShoot.get()) return null;
            blockShoot.set(true);
        }

        consumeEnderPearl(player);

        Projectile projectile = player.launchProjectile(EnderPearl.class, player.getLocation().getDirection().clone().normalize().multiply(projectileSpeed));
        projectile.setGravity(gravity);

        projectile.setMetadata("ability", new FixedMetadataValue(plugin, player.getName()));

        player.setCooldown(Material.ENDER_PEARL, 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (projectile.isValid()) {
                    projectile.remove();
                }
                if (blockShoot != null)
                    blockShoot.set(false);
            }
        }.runTaskLater(plugin, projectileLifetime);

        return projectile;
    }

    public static Location fixProjectileHitLocation(Player player, Projectile projectile, double projectileSpeed) {
        Location fixedLocation = projectile.getLocation();
        Vector offset = projectile.getVelocity().clone().normalize().multiply(0.5);
        double distance = 0;

        World world = player.getWorld();
        while (!world.getBlockAt(fixedLocation).getType().isSolid() && distance < projectileSpeed) {
            fixedLocation.add(offset);
            distance += 0.5;
        }
        return fixedLocation;
    }
}
