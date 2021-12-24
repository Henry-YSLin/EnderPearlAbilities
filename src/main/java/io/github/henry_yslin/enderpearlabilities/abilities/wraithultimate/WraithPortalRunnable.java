package io.github.henry_yslin.enderpearlabilities.abilities.wraithultimate;

import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.managers.voidspace.VoidSpaceManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.List;

public class WraithPortalRunnable extends AbilityRunnable {
    WraithUltimateAbility ability;
    int pathIdx;
    BossBar bossBar = null;
    Location[] locations;
    List<Location> path;
    LivingEntity livingEntity;
    boolean reverse;

    public WraithPortalRunnable(WraithUltimateAbility ability, Location[] locations, List<Location> path, LivingEntity livingEntity, boolean reverse) {
        this.ability = ability;
        this.locations = locations;
        this.path = path;
        this.livingEntity = livingEntity;
        this.reverse = reverse;
    }

    @Override
    protected void start() {
        livingEntity.setMetadata("wraith-portal", new FixedMetadataValue(executor.getPlugin(), executor));
        if (livingEntity instanceof Player player) {
            bossBar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + ability.getInfo().getName(), BarColor.PURPLE, BarStyle.SOLID, BarFlag.CREATE_FOG, BarFlag.DARKEN_SKY);
            bossBar.setProgress(0);
            bossBar.addPlayer(player);
        }
        livingEntity.getWorld().playSound(livingEntity.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.3f, 2);

        VoidSpaceManager.getInstance().enterVoid(livingEntity);

        pathIdx = reverse ? path.size() - 1 : 0;
        livingEntity.setVelocity(path.get(pathIdx).clone().subtract(livingEntity.getLocation()).toVector());
    }

    @Override
    protected void tick() {
        if (!livingEntity.isValid()) {
            cancel();
            return;
        }
        if (bossBar != null) {
            if (reverse)
                bossBar.setProgress(1 - (float) pathIdx / path.size());
            else
                bossBar.setProgress(pathIdx / (float) path.size());
        }
        if (reverse && pathIdx <= 0 || !reverse && pathIdx >= path.size() - 1) {
            cancel();
            return;
        }
        Location loc = path.get(pathIdx).clone();
        Vector entityDirection = livingEntity.getLocation().getDirection();
        livingEntity.teleport(loc.setDirection(loc.getDirection().multiply(reverse ? -1 : 1).subtract(entityDirection).multiply(0.25).add(entityDirection)));
        if (reverse)
            pathIdx--;
        else
            pathIdx++;
        livingEntity.setVelocity(path.get(pathIdx).clone().subtract(livingEntity.getLocation()).toVector());
    }

    @Override
    protected void end() {
        livingEntity.removeMetadata("wraith-portal", executor.getPlugin());
        if (bossBar != null) {
            bossBar.removeAll();
        }
        int end = reverse ? 0 : 1;
        boolean completed = livingEntity.getLocation().distance(locations[end]) < 1.5;
        if (completed)
            livingEntity.teleport(locations[end].clone().setDirection(livingEntity.getLocation().getDirection()));

        livingEntity.setVelocity(new Vector());
        livingEntity.setFallDistance(0);

        VoidSpaceManager.getInstance().exitVoid(livingEntity);
    }
}
