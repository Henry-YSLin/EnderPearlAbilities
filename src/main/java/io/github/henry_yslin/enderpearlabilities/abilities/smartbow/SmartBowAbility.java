package io.github.henry_yslin.enderpearlabilities.abilities.smartbow;

import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henry_yslin.enderpearlabilities.managers.interactionlock.InteractionLockManager;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.EntityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.PlayerUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
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

public class SmartBowAbility extends Ability {

    static final int MAX_AIR_TIME = 100;
    static final double LOCK_ON_RANGE = 100;
    static final double LOCK_ON_RADIUS = 10;
    static final double LOCK_ON_ANGLE = 45d / 180 * Math.PI;

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        super.setConfigDefaults(config);
        config.addDefault("charge-up", 10);
        config.addDefault("duration", 12);
        config.addDefault("cooldown", 100);
    }

    public SmartBowAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("smart-bow")
                .name("Smart Bow")
                .origin("Titanfall")
                .description("Enhance your bow-type weapons with advanced automatic aim correction that greatly increases the chance of hitting your target.")
                .usage("Right click with an ender pearl to activate. Right click again to cancel. Cooldown depends on the number of shots fired.")
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
    final AtomicInteger shotsLeft = new AtomicInteger(0);
    final BossBar bossbar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SEGMENTED_12);

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
        bossbar.setTitle(ChatColor.BLUE + info.name);
        chargingUp.set(false);
        abilityActive.set(false);
        shotsLeft.set(0);
        bossbar.setVisible(false);
        bossbar.addPlayer(player);
        cooldown.startCooldown(info.cooldown);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        bossbar.removeAll();
    }

    private void simulate1TickMovement(Entity entity, Location location, Vector velocity, double gravity, double drag) {
        if (entity.isOnGround()) {
            location.add(velocity.clone().setY(0));
        } else if (EntityUtils.isFlying(entity)) {
            location.add(velocity);
        } else {
            boolean onGround = false;
            if (velocity.getY() <= 0) {
                RayTraceResult rayTraceResult = entity.getWorld().rayTraceBlocks(new Location(location.getWorld(), location.getX(), location.getY() + 0.01, location.getZ()), new Vector(0, -1, 0), 0.02 + velocity.getY(), FluidCollisionMode.ALWAYS, true);
                if (rayTraceResult != null && rayTraceResult.getHitBlock() != null)
                    onGround = true;
            }
            if (onGround)
                velocity.multiply(0);
            location.add(velocity);
            if (EntityUtils.hasDelayedDrag(entity)) {
                velocity.add(new Vector(0, -gravity, 0));
                velocity.multiply(1 - drag);
            } else {
                velocity.multiply(1 - drag);
                velocity.add(new Vector(0, -gravity, 0));
            }
        }
    }

    private Vector computeProjectileVelocity(Entity projectile, LivingEntity target, double maxVelocity, int maxAirTime) {
        double projectileGravity = EntityUtils.getGravity(projectile);
        double projectileDrag = EntityUtils.getDrag(projectile);
        double targetGravity = EntityUtils.getGravity(target);
        double targetDrag = EntityUtils.getDrag(target);
        double aimHeight = target.getEyeHeight();

        Location targetLocation = target.getLocation();
        Vector targetVelocity = target.getVelocity();

        double cumulativeGravity = 0;

        for (int i = 1; i <= maxAirTime; i++) {
            simulate1TickMovement(target, targetLocation, targetVelocity, targetGravity, targetDrag);

            double dragCoefficient = (1 - Math.pow(1 - projectileDrag, i)) / (projectileDrag);

            Location loc = projectile.getLocation();
            loc.setY(targetLocation.getY());
            double horizontalDistance = targetLocation.distance(loc);
            double horizontalVelocity = horizontalDistance / dragCoefficient;

            double verticalDistance = targetLocation.getY() + aimHeight - projectile.getLocation().getY();
            double verticalVelocity = (verticalDistance + cumulativeGravity) / dragCoefficient;

            cumulativeGravity += projectileGravity * dragCoefficient;

            if (horizontalVelocity * horizontalVelocity + verticalVelocity * verticalVelocity > maxVelocity * maxVelocity)
                continue;

            return targetLocation.toVector().subtract(projectile.getLocation().toVector().setY(targetLocation.getY())).normalize().multiply(horizontalVelocity).add(new Vector(0, verticalVelocity, 0));
        }
        return target.getLocation().toVector().subtract(projectile.getLocation().toVector()).normalize().multiply(maxVelocity);
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
        bossbar.setProgress(shots / (double) info.duration);

        if (shots <= 0) {
            abilityActive.set(false);
            bossbar.setVisible(false);
            cooldown();
        }

        RayTraceResult result = rayTrace(0, arrow);
        if (result == null || result.getHitEntity() == null)
            result = rayTrace(LOCK_ON_RADIUS, arrow);
        if (result != null && result.getHitEntity() != null) {
            LivingEntity target = (LivingEntity) result.getHitEntity();
            Vector velocity = computeProjectileVelocity(arrow, target, arrow.getVelocity().length(), MAX_AIR_TIME);
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
        cooldown.startCooldown(info.cooldown + (info.duration - shotsLeft.get()) * 40);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation, true)) return;

        event.setCancelled(true);

        if (player.getName().equals(ownerName) && abilityActive.get()) {
            bossbar.setVisible(false);
            abilityActive.set(false);
            cooldown();
            shotsLeft.set(0);
            return;
        }

        if (InteractionLockManager.getInstance().isInteractionLocked(player)) return;

        if (cooldown.getCoolingDown()) return;
        if (chargingUp.get()) return;

        new FunctionChain(
                next -> {
                    PlayerUtils.consumeEnderPearl(player);
                    next.run();
                },
                next -> AbilityUtils.chargeUpSequence(this, player, info.chargeUp, chargingUp, next),
                next -> {
                    abilityActive.set(true);
                    bossbar.setVisible(true);
                    bossbar.setProgress(1);
                    shotsLeft.set(info.duration);
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
