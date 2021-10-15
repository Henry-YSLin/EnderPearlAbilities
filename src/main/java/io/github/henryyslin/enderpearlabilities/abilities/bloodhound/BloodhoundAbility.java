package io.github.henryyslin.enderpearlabilities.abilities.bloodhound;

import io.github.henryyslin.enderpearlabilities.abilities.Ability;
import io.github.henryyslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henryyslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henryyslin.enderpearlabilities.utils.AbilityRunnable;
import io.github.henryyslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henryyslin.enderpearlabilities.utils.FunctionChain;
import io.github.henryyslin.enderpearlabilities.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
        abilityActive.set(false);
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;
        if (abilityActive.get()) return;

        AbilityUtils.consumeEnderPearl(player);

        List<Entity> entities = new ArrayList<>();
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Team tmp = null;
        if (manager != null) {
            Scoreboard scoreboard = manager.getMainScoreboard();
            tmp = scoreboard.registerNewTeam(StringUtils.substring("bh_" + ownerName, 0, 16));
            tmp.setColor(ChatColor.RED);
        }
        final Team team = tmp;

        new FunctionChain(
                next -> {
                    abilityActive.set(true);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1, 0);
                    player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 5000, 1, 1, 1, 3);
                    next.run();
                },
                next -> AbilityUtils.chargeUpSequence(this, player, info.chargeUp, next),
                next -> {
                    entities.addAll(player.getWorld().getNearbyEntities(player.getLocation(), SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS));
                    entities.removeIf(entity -> entity.getUniqueId().equals(player.getUniqueId()));
                    for (Entity entity : entities) {
                        entity.setGlowing(true);
                        if (team != null)
                            if (entity instanceof LivingEntity)
                                team.addEntry(entity.getUniqueId().toString());
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
