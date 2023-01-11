package io.github.henry_yslin.enderpearlabilities.abilities.timefreeze;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityCooldown;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.SingleUseCooldown;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.WorldUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TimeFreezeAbility extends Ability<TimeFreezeAbilityInfo> {

    static final int FREEZE_RANGE = 30;

    public TimeFreezeAbility(Plugin plugin, TimeFreezeAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);

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
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    static record FreezeRecord(Entity entity, Location location, boolean gravity, Vector velocity, boolean hasAI,
                               int fuseTick) {
    }

    private FreezeRecord freezeEntity(Entity entity) {
        FreezeRecord record = new FreezeRecord(entity, entity.getLocation(), entity.hasGravity(), entity.getVelocity(), entity instanceof LivingEntity livingEntity && livingEntity.hasAI(), entity instanceof Creeper creeper ? creeper.getFuseTicks() : 0);
        entity.setInvulnerable(true);
        entity.setGravity(false);
        entity.setVelocity(new Vector(0, 0, 0));
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.setCollidable(false);
            livingEntity.setAI(false);
        }
        return record;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;

        event.setCancelled(true);

        if (!cooldown.isAbilityUsable()) return;
        if (abilityActive.get()) return;
        if (chargingUp.get()) return;

        List<FreezeRecord> entities = new ArrayList<>();

        new FunctionChain(
                next -> {
                    AbilityUtils.consumeEnderPearl(this, player);
                    EnderPearlAbilities.getInstance().emitEvent(
                            EventListener.class,
                            new AbilityActivateEvent(this),
                            EventListener::onAbilityActivate
                    );
                    next.run();
                },
                next -> AbilityUtils.chargeUpSequence(this, player, info.getChargeUp(), chargingUp, next),
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
                        bossbar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.getName(), BarColor.PURPLE, BarStyle.SOLID);
                        bossbar.addPlayer(player);
                    }

                    @Override
                    protected synchronized void tick() {
                        if (!abilityActive.get() || !player.isValid()) {
                            cancel();
                        }
                        bossbar.setProgress(count / (double) info.getDuration());
                        for (FreezeRecord record : entities) {
                            record.entity.setVelocity(new Vector(0, 0, 0));
                            record.entity.teleport(record.location);
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
                }.runTaskRepeated(this, 0, 1, info.getDuration()),
                next -> {
                    abilityActive.set(false);

                    for (FreezeRecord record : entities) {
                        record.entity.setInvulnerable(false);
                        record.entity.setGravity(record.gravity);
                        record.entity.setVelocity(record.velocity);
                        if (record.entity instanceof LivingEntity livingEntity) {
                            livingEntity.setCollidable(true);
                            livingEntity.setAI(record.hasAI);
                        }
                    }

                    cooldown.setCooldown(info.getCooldown());
                    next.run();
                }
        ).execute();
    }
}
