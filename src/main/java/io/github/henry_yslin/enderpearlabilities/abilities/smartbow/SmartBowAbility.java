package io.github.henry_yslin.enderpearlabilities.abilities.smartbow;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityCooldown;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.SingleUseCooldown;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.managers.abilitylock.AbilityLockManager;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.ProjectileUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SmartBowAbility extends Ability<SmartBowAbilityInfo> {

    static final int MAX_AIR_TIME = 100;
    static final double LOCK_ON_RANGE = 100;
    static final double LOCK_ON_RADIUS = 10;
    static final double LOCK_ON_ANGLE = 45d / 180 * Math.PI;

    public SmartBowAbility(Plugin plugin, SmartBowAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    final AtomicInteger shotsLeft = new AtomicInteger(0);
    final BossBar bossbar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SEGMENTED_12);

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
        bossbar.setTitle(ChatColor.BLUE + info.getName());
        chargingUp.set(false);
        abilityActive.set(false);
        shotsLeft.set(0);
        bossbar.setVisible(false);
        bossbar.addPlayer(player);
        cooldown.setCooldown(info.getCooldown());
    }

    @Override
    public void onDisable() {
        super.onDisable();
        bossbar.removeAll();
    }

    private RayTraceResult rayTrace(double raySize, Entity arrow) {
        return arrow.getWorld().rayTraceEntities(arrow.getLocation(), arrow.getVelocity(), LOCK_ON_RANGE, raySize, entity -> {
            if (!entity.isValid()) return false;
            if (entity instanceof Player p) {
                if (p.getGameMode() == GameMode.SPECTATOR) return false;
            }
            if (entity.equals(player) || entity.equals(arrow) || !(entity instanceof LivingEntity livingEntity))
                return false;
            if (raySize > 0) {
                return livingEntity.getEyeLocation().subtract(arrow.getLocation()).toVector().angle(arrow.getVelocity()) <= LOCK_ON_ANGLE;
            }
            return true;
        });
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.getName().equals(ownerName)) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        if (!abilityActive.get()) return;

        int shots = shotsLeft.decrementAndGet();
        bossbar.setProgress(shots / (double) info.getMagazineSize());

        if (shots <= 0) {
            abilityActive.set(false);
            bossbar.setVisible(false);
            cooldown();
        }

        RayTraceResult result = rayTrace(1, arrow);
        if (result == null || result.getHitEntity() == null)
            result = rayTrace(LOCK_ON_RADIUS, arrow);
        if (result != null && result.getHitEntity() != null) {
            LivingEntity target = (LivingEntity) result.getHitEntity();
            Vector velocity = ProjectileUtils.computeProjectileVelocity(arrow, target, arrow.getVelocity().length(), MAX_AIR_TIME);
            arrow.setVelocity(velocity);
            player.spawnParticle(Particle.FLASH, target.getLocation(), 1, 0, 0, 0, 0);
        } else {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No target found"));
        }

        Vector velocity = arrow.getVelocity();
        Location location = arrow.getLocation();

        for (int i = 0; i < 100; i++) {
            location.add(velocity);
            velocity.multiply(0.99);
            velocity.add(new Vector(0, -1 / 20f, 0));
            if (i >= 2)
                player.spawnParticle(Particle.DRAGON_BREATH, location, 1, 0, 0, 0, 0);
        }

        (new AbilityRunnable() {
            @Override
            protected void tick() {
                super.tick();
                if (arrow.isOnGround() || !arrow.isValid()) {
                    cancel();
                    return;
                }
                if (arrow.getTicksLived() > 2)
                    arrow.getWorld().spawnParticle(Particle.END_ROD, arrow.getLocation(), 1, 0, 0, 0, 0, null, true);
            }
        }).runTaskTimer(this, 0, 1);
    }

    private void cooldown() {
        cooldown.setCooldown(info.getBaseCooldown() + (info.getMagazineSize() - shotsLeft.get()) * info.getCooldownPerShot());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation(), true)) return;

        event.setCancelled(true);

        if (player.getName().equals(ownerName) && abilityActive.get()) {
            bossbar.setVisible(false);
            abilityActive.set(false);
            cooldown();
            shotsLeft.set(0);
            return;
        }

        if (AbilityLockManager.getInstance().isAbilityLocked(player)) return;

        if (!cooldown.isAbilityUsable()) return;
        if (chargingUp.get()) return;

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
                    bossbar.setVisible(true);
                    bossbar.setProgress(1);
                    shotsLeft.set(info.getMagazineSize());
                }
        ).execute();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer().getName().equals(ownerName)) {
            bossbar.setVisible(false);
            abilityActive.set(false);
            chargingUp.set(false);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getEntity().getName().equals(ownerName)) {
            bossbar.setVisible(false);
            abilityActive.set(false);
            chargingUp.set(false);
            cooldown();
        }
    }
}
