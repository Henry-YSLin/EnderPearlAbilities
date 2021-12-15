package io.github.henry_yslin.enderpearlabilities.abilities.timefreeze;

import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.PlayerUtils;
import io.github.henry_yslin.enderpearlabilities.utils.WorldUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO: name subject to change
public class TimeFreezeAbility extends Ability {

    static final int FREEZE_RANGE = 30;

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        super.setConfigDefaults(config);
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 100);
        config.addDefault("cooldown", 600);
    }

    public TimeFreezeAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("time-freeze")
                .name("Time Freeze")
                .origin("Original")
                .description("Locally freeze time for a short duration.")
                .usage("Right click with an ender pearl to activate the ability. Frozen entities are invulnerable. Players are not affected by the freeze.")
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

    @Override
    public void onDisable() {
        super.onDisable();
    }

    static record FreezeRecord(Entity entity, boolean gravity, Vector velocity, boolean hasAI, int fuseTick) {
    }

    private FreezeRecord freezeEntity(Entity entity) {
        FreezeRecord record = new FreezeRecord(entity, entity.hasGravity(), entity.getVelocity(), entity instanceof Mob mob && mob.hasAI(), entity instanceof Creeper creeper ? creeper.getFuseTicks() : 0);
        entity.setInvulnerable(true);
        entity.setGravity(false);
        entity.setVelocity(new Vector(0, 0, 0));
        if (entity instanceof Mob mob) {
            mob.setAI(false);
        }
        return record;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;
        if (abilityActive.get()) return;
        if (chargingUp.get()) return;

        List<FreezeRecord> entities = new ArrayList<>();

        new FunctionChain(
                next -> {
                    PlayerUtils.consumeEnderPearl(player);
                    next.run();
                },
                next -> AbilityUtils.chargeUpSequence(this, player, info.chargeUp, chargingUp, next),
                next -> {
                    abilityActive.set(true);

                    for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), FREEZE_RANGE, FREEZE_RANGE, FREEZE_RANGE, entity -> !(entity instanceof Player))) {
                        entities.add(freezeEntity(entity));
                    }
                    WorldUtils.spawnParticleCubeOutline(player.getLocation().subtract(FREEZE_RANGE, FREEZE_RANGE, FREEZE_RANGE), player.getLocation().add(FREEZE_RANGE, FREEZE_RANGE, FREEZE_RANGE), Particle.END_ROD, 1, true);

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
                        if (!abilityActive.get() || !player.isValid()) {
                            cancel();
                        }
                        bossbar.setProgress(count / (double) info.duration);
                        for (FreezeRecord record : entities) {
                            if (record.entity instanceof Creeper creeper) {
                                creeper.setFuseTicks(record.fuseTick);
                            }
                        }
                        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), FREEZE_RANGE, FREEZE_RANGE, FREEZE_RANGE, entity -> !(entity instanceof Player) && entities.stream().noneMatch(record -> record.entity.equals(entity)))) {
                            entities.add(freezeEntity(entity));
                        }
                    }

                    @Override
                    protected synchronized void end() {
                        bossbar.removeAll();
                        next.run();
                    }
                }.runTaskRepeated(this, 0, 1, info.duration),
                next -> {
                    abilityActive.set(false);

                    for (FreezeRecord record : entities) {
                        record.entity.setInvulnerable(false);
                        record.entity.setGravity(record.gravity);
                        record.entity.setVelocity(record.velocity);
                        if (record.entity instanceof Mob mob) {
                            mob.setAI(record.hasAI);
                        }
                    }

                    cooldown.startCooldown(info.cooldown);
                    next.run();
                }
        ).execute();
    }
}
