package io.github.henryyslin.enderpearlabilities.abilities.seer;

import io.github.henryyslin.enderpearlabilities.abilities.Ability;
import io.github.henryyslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henryyslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henryyslin.enderpearlabilities.utils.AbilityRunnable;
import io.github.henryyslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henryyslin.enderpearlabilities.utils.FunctionChain;
import io.github.henryyslin.enderpearlabilities.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
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
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SeerAbility extends Ability {
    static final double SCAN_RADIUS = 4;
    static final double SCAN_RANGE = 75;
    static final int PARTICLE_COUNT = 750;
    static final double ANGLE_DELTA = 1;
    static final int SPREAD_TIME = 5;

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 30);
        config.addDefault("duration", 200);
        config.addDefault("cooldown", 500);
    }

    public SeerAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("seer")
                .name("Focus of Attention")
                .origin("Apex - Seer")
                .description("Summon micro-drones to emit a delayed blast that goes through walls blinding and revealing entities.\nPassive ability: Visualize the heartbeats of nearby entities when sneaking.")
                .usage("Right click to activate. Entities hit by the blast receive debuffs and remain revealed for the duration of the ability. If no entities are hit, the ability goes into cooldown early.")
                .activation(ActivationHand.MainHand);

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
    HeartSeekerRunnable heartSeekerRunnable;

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
        if (heartSeekerRunnable != null && !heartSeekerRunnable.isCancelled())
            heartSeekerRunnable.cancel();
        heartSeekerRunnable = new HeartSeekerRunnable(player);
        heartSeekerRunnable.runTaskTimer(this, 0, 5);
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;
        if (abilityActive.get()) return;

        abilityActive.set(true);

        Location origin = player.getLocation();

        new FunctionChain(
                next -> {
                    new CylindricalParticleEffect(Particle.ELECTRIC_SPARK, origin, origin.getDirection(), SCAN_RANGE, SCAN_RADIUS, ANGLE_DELTA, PARTICLE_COUNT)
                            .runTaskRepeated(this, 0, 1, SPREAD_TIME);
                    new CylindricalParticleEffect(Particle.DRAGON_BREATH, origin, origin.getDirection(), SCAN_RANGE, SCAN_RADIUS, ANGLE_DELTA, PARTICLE_COUNT)
                            .runTaskRepeated(this, 0, 1, SPREAD_TIME);
                    next.run();
                },
                next -> AbilityUtils.delay(this, info.chargeUp, () -> {
                    new CylindricalParticleEffect(Particle.END_ROD, origin, origin.getDirection(), SCAN_RANGE, SCAN_RADIUS, ANGLE_DELTA, PARTICLE_COUNT)
                            .runTaskRepeated(this, 0, 1, 1);
                    next.run();
                }, false),
                next -> {
                    if (origin.getWorld() == null) return;
                    List<Entity> entities = new ArrayList<>();
                    RayTraceResult result;
                    do {
                        result = origin.getWorld().rayTraceEntities(origin, origin.getDirection(), SCAN_RANGE, SCAN_RADIUS, entity -> !entities.contains(entity) && !entity.equals(player));
                        if (result != null && result.getHitEntity() != null)
                            entities.add(result.getHitEntity());
                    } while (result != null);

                    if (entities.size() == 0) {
                        abilityActive.set(false);
                        cooldown.startCooldown(info.cooldown);
                        return;
                    }

                    ScoreboardManager manager = Bukkit.getScoreboardManager();
                    Team tmp = null;
                    if (manager != null) {
                        Scoreboard scoreboard = manager.getMainScoreboard();
                        tmp = scoreboard.registerNewTeam(StringUtils.substring("se_" + ownerName, 0, 16));
                        tmp.setColor(ChatColor.BLUE);
                    }
                    final Team team = tmp;

                    for (Entity entity : entities) {
                        entity.setGlowing(true);
                        if (entity instanceof LivingEntity livingEntity) {
                            if (team != null)
                                team.addEntry(entity.getUniqueId().toString());
                            livingEntity.addPotionEffect(PotionEffectType.BLINDNESS.createEffect(info.duration, 1));
                            livingEntity.addPotionEffect(PotionEffectType.WEAKNESS.createEffect(info.duration, 1));
                            livingEntity.addPotionEffect(PotionEffectType.SLOW.createEffect(info.duration, 2));
                        }
                        if (entity instanceof Player p) {
                            player.sendTitle(" ", ChatColor.RED + "Micro-drones detected", 5, 30, 30);
                        }
                    }

                    new AbilityRunnable() {
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
                            abilityActive.set(false);
                            cooldown.startCooldown(info.cooldown);
                            next.run();
                        }
                    }.runTaskRepeated(this, 0, 10, info.duration / 10);
                }
        ).execute();
    }
}