package io.github.henry_yslin.enderpearlabilities.abilities.bloodhoundtactical;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityCooldown;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.SingleUseCooldown;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.StringUtils;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class BloodhoundTacticalAbility extends Ability<BloodhoundTacticalAbilityInfo> {

    static final double FORWARD_SCAN_RADIUS = 75;
    static final double PERIPHERY_SCAN_RADIUS = 16;
    static final double FORWARD_FOV_ANGLE = 125.0 / 2 / 180 * Math.PI;

    public BloodhoundTacticalAbility(Plugin plugin, BloodhoundTacticalAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    TrackerRunnable trackerRunnable;

    @Override
    protected AbilityCooldown createCooldown() {
        return new SingleUseCooldown(this, player);
    }

    @Override
    public boolean isActive() {
        return abilityActive.get();
    }

    @Override
    public boolean isChargingUp() {
        return chargingUp.get();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        super.onPlayerJoin(event);
        Player player = event.getPlayer();
        if (player.getName().equals(ownerName)) {
            setUpPlayer(player);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (player != null) {
            setUpPlayer(player);
        }
    }

    private void setUpPlayer(Player player) {
        chargingUp.set(false);
        abilityActive.set(false);
        cooldown.setCooldown(info.getCooldown());

        if (trackerRunnable != null && !trackerRunnable.isCancelled())
            trackerRunnable.cancel();
        trackerRunnable = new TrackerRunnable(player);
        trackerRunnable.runTaskTimer(this, 0, 10);
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;

        event.setCancelled(true);

        if (!cooldown.isAbilityUsable()) return;
        if (chargingUp.get()) return;
        if (abilityActive.get()) return;

        AbilityUtils.consumeEnderPearl(this, player);
        EnderPearlAbilities.getInstance().emitEvent(
                EventListener.class,
                new AbilityActivateEvent(this),
                EventListener::onAbilityActivate
        );

        List<Entity> entities = new ArrayList<>();
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Team tmp = null;
        if (manager != null) {
            String teamName = StringUtils.substring("bh_" + ownerName, 0, 16);
            Scoreboard scoreboard = manager.getMainScoreboard();
            tmp = scoreboard.getTeam(teamName);
            if (tmp != null)
                tmp.unregister();
            tmp = scoreboard.registerNewTeam(teamName);
            tmp.setColor(ChatColor.RED);
        }
        final Team team = tmp;

        new FunctionChain(
                next -> AbilityUtils.chargeUpSequence(this, player, info.getChargeUp(), chargingUp, next),
                next -> {
                    abilityActive.set(true);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1, 0);
                    player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 3000, 1, 1, 1, 2);
                    entities.addAll(player.getWorld().getNearbyEntities(player.getLocation(), FORWARD_SCAN_RADIUS, FORWARD_SCAN_RADIUS, FORWARD_SCAN_RADIUS, entity -> {
                        Location location = entity.getLocation();
                        if (location.distanceSquared(player.getLocation()) <= PERIPHERY_SCAN_RADIUS * PERIPHERY_SCAN_RADIUS)
                            return true;
                        if (location.subtract(player.getLocation()).toVector().angle(player.getLocation().getDirection()) <= FORWARD_FOV_ANGLE)
                            return true;
                        return false;
                    }));
                    entities.removeIf(entity -> {
                        if (entity instanceof Player p) {
                            if (p.getGameMode() == GameMode.SPECTATOR) return true;
                        }
                        return entity.getUniqueId().equals(player.getUniqueId());
                    });
                    if (entities.isEmpty()) {
                        if (team != null)
                            team.unregister();
                        entities.clear();
                        abilityActive.set(false);
                        cooldown.setCooldown(info.getCooldown());
                        return;
                    }
                    for (Entity entity : entities) {
                        if (entity instanceof LivingEntity livingEntity) {
                            if (team != null)
                                team.addEntry(entity.getUniqueId().toString());
                            livingEntity.addPotionEffect(PotionEffectType.GLOWING.createEffect(info.getDuration(), 1));
                        } else {
                            entity.setGlowing(true);
                        }
                        if (entity instanceof Player p) {
                            p.sendTitle(" ", ChatColor.RED + "Sonar detected", 5, 30, 30);
                        }
                    }
                    next.run();
                },
                next -> new AbilityRunnable() {
                    BossBar bossbar;

                    @Override
                    protected synchronized void start() {
                        bossbar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.getName(), BarColor.PURPLE, BarStyle.SOLID);
                        bossbar.addPlayer(player);
                    }

                    @Override
                    protected synchronized void tick() {
                        if (!abilityActive.get()) {
                            cancel();
                        }
                        bossbar.setProgress(count / (double) info.getDuration() * 10);
                    }

                    @Override
                    protected synchronized void end() {
                        bossbar.removeAll();
                        for (Entity entity : entities) {
                            if (!(entity instanceof LivingEntity)) // LivingEntities will have their glow disabled through potion effect timeout
                                entity.setGlowing(false);
                            if (team != null)
                                team.removeEntry(entity.getUniqueId().toString());
                        }
                        if (team != null)
                            team.unregister();
                        entities.clear();
                        abilityActive.set(false);
                        cooldown.setCooldown(info.getCooldown());
                        next.run();
                    }
                }.runTaskRepeated(this, 0, 10, info.getDuration() / 10)
        ).execute();
    }
}
