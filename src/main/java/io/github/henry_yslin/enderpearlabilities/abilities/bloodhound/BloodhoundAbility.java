package io.github.henry_yslin.enderpearlabilities.abilities.bloodhound;

import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henry_yslin.enderpearlabilities.utils.*;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
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

public class BloodhoundAbility extends Ability {
    static final double SCAN_RADIUS = 75;

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 10);
        config.addDefault("duration", 200);
        config.addDefault("cooldown", 600);
    }

    public BloodhoundAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("bloodhound")
                .name("Eye of the Allfather")
                .origin("Apex - Bloodhound")
                .description("Briefly reveal entities through all structures around you.")
                .usage("Right click to activate. Living entities are marked red while others are marked white. Scanned players are warned through a pop-up message.")
                .activation(ActivationHand.OffHand);

        if (config != null)
            builder
                    .chargeUp(config.getInt("charge-up"))
                    .duration(config.getInt("duration"))
                    .cooldown(config.getInt("cooldown"));

        info = builder.build();
    }

    @Override
    public AbilityInfo getInfo() {
        return info;
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);

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
        cooldown.startCooldown(info.cooldown);
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;
        if (chargingUp.get()) return;
        if (abilityActive.get()) return;

        PlayerUtils.consumeEnderPearl(player);

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
                next -> AbilityUtils.chargeUpSequence(this, player, info.chargeUp, chargingUp, next),
                next -> {
                    abilityActive.set(true);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1, 0);
                    player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 5000, 1, 1, 1, 3);
                    entities.addAll(player.getWorld().getNearbyEntities(player.getLocation(), SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS));
                    entities.removeIf(entity -> {
                        if (entity instanceof Player p) {
                            if (p.getGameMode() == GameMode.SPECTATOR) return true;
                        }
                        return entity.getUniqueId().equals(player.getUniqueId());
                    });
                    for (Entity entity : entities) {
                        if (entity instanceof LivingEntity livingEntity) {
                            if (team != null)
                                team.addEntry(entity.getUniqueId().toString());
                            livingEntity.addPotionEffect(PotionEffectType.GLOWING.createEffect(info.duration, 1));
                        } else {
                            entity.setGlowing(true);
                        }
                        if (entity instanceof Player p) {
                            player.sendTitle(" ", ChatColor.RED + "Sonar detected", 5, 30, 30);
                        }
                    }
                    next.run();
                },
                next -> new AbilityRunnable() {
                    BossBar bossbar;

                    @Override
                    protected synchronized void start() {
                        bossbar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.name, BarColor.PURPLE, BarStyle.SOLID);
                        bossbar.addPlayer(player);
                    }

                    @Override
                    protected synchronized void tick() {
                        if (!abilityActive.get()) {
                            cancel();
                        }
                        bossbar.setProgress(count / (double) info.duration * 10);
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
                        cooldown.startCooldown(info.cooldown);
                        next.run();
                    }
                }.runTaskRepeated(this, 0, 10, info.duration / 10)
        ).execute();
    }
}
