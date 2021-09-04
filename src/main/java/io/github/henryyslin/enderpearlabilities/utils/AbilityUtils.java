package io.github.henryyslin.enderpearlabilities.utils;

import io.github.henryyslin.enderpearlabilities.ActivationHand;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

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
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }
}
