package io.github.henry_yslin.enderpearlabilities.abilities.horizonultimate;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityCouple;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.utils.*;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.concurrent.atomic.AtomicBoolean;

public class HorizonUltimateAbility extends Ability<HorizonUltimateAbilityInfo> {

    static final int PROJECTILE_LIFETIME = 100;
    static final double PROJECTILE_SPEED = 2;
    static final boolean PROJECTILE_GRAVITY = true;

    public HorizonUltimateAbility(Plugin plugin, HorizonUltimateAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean blockShoot = new AtomicBoolean(false);
    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);

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
        abilityActive.set(false);
        blockShoot.set(false);
        cooldown.setCooldown(info.getCooldown());
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;

        event.setCancelled(true);

        if (cooldown.isCoolingDown()) return;
        if (abilityActive.get()) return;

        AbilityUtils.fireProjectile(this, player, blockShoot, PROJECTILE_LIFETIME, PROJECTILE_SPEED, PROJECTILE_GRAVITY);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!AbilityUtils.verifyAbilityCouple(this, event.getEntity())) return;
        event.getDrops().clear();
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();

        if (!(shooter instanceof Player player)) return;
        if (!AbilityUtils.verifyAbilityCouple(this, projectile)) return;
        if (!player.getName().equals(ownerName)) return;

        if (!(projectile instanceof Snowball)) return;

        event.setCancelled(true);

        AbilityUtils.consumeEnderPearl(this, player);
        EnderPearlAbilities.getInstance().emitEvent(
                EventListener.class,
                new AbilityActivateEvent(this),
                EventListener::onAbilityActivate
        );

        projectile.remove();
        blockShoot.set(false);

        Location finalLocation = ProjectileUtils.correctProjectileHitLocation(projectile);

        Shulker device = projectile.getWorld().spawn(finalLocation, Shulker.class, entity -> {
            entity.setAI(false);
            entity.setSilent(true);
            entity.setRemoveWhenFarAway(false);
            entity.setPeek(0);
            entity.setColor(DyeColor.PURPLE);
            entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.getCodeName(), ownerName)));
        });

        WorldUtils.spawnParticleCubeOutline(finalLocation.clone().add(-5.5, -5.5, -5.5), finalLocation.clone().add(5.5, 5.5, 5.5), Particle.VILLAGER_HAPPY, 5, true);

        World world = projectile.getWorld();

        new FunctionChain(
                nextFunction -> new AbilityRunnable() {
                    @Override
                    protected void start() {
                        chargingUp.set(true);
                    }

                    @Override
                    protected void tick() {
                        if (!device.isValid()) {
                            cancel();
                            return;
                        }
                        device.setPeek(1f - count / (float) info.getChargeUp() * 2);
                        world.playSound(device.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1, 0);
                        world.spawnParticle(Particle.EXPLOSION_NORMAL, finalLocation, 2, 0.5, 0.5, 0.5, 0.02);
                    }

                    @Override
                    protected void end() {
                        chargingUp.set(false);
                        device.setPeek(1);
                        if (this.hasCompleted())
                            nextFunction.run();
                    }
                }.runTaskRepeated(this, 0, 2, info.getChargeUp() / 2),
                nextFunction -> new AbilityRunnable() {
                    BossBar bossBar;

                    @Override
                    protected void start() {
                        abilityActive.set(true);
                        world.playSound(device.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1, 0);
                        bossBar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.getName(), BarColor.PURPLE, BarStyle.SOLID);
                        bossBar.addPlayer(player);
                    }

                    @Override
                    protected void tick() {
                        if (!device.isValid()) {
                            cancel();
                            return;
                        }
                        Location deviceLocation = device.getLocation();
                        bossBar.setProgress((double) count / info.getDuration());
                        world.getNearbyEntities(deviceLocation, 5.5, 5.5, 5.5).forEach(entity -> {
                            if (entity == device) return;
                            if (entity instanceof Player player && player.getGameMode() == GameMode.SPECTATOR) return;
                            Vector velocity = MathUtils.clamp(entity.getVelocity(), 0.8);
                            velocity = MathUtils.replaceInfinite(velocity, new Vector(0, 0, 0));
                            Vector blackHole = deviceLocation.toVector().subtract(entity.getLocation().toVector());
                            double distance = blackHole.length();
                            blackHole.normalize().multiply(Math.min(1, 1.25 / distance));
                            blackHole = MathUtils.replaceInfinite(blackHole, new Vector(0, 0, 0));
                            entity.setVelocity(MathUtils.clamp(velocity.add(blackHole), 0.6));
                            if (count % 10 == 0)
                                if (entity instanceof LivingEntity livingEntity)
                                    livingEntity.damage(1, player);
                        });
                        world.spawnParticle(Particle.SMOKE_LARGE, deviceLocation, 5, 0.5, 0.5, 0.5, 0.2);
                        if (count % 20 == 19) {
                            WorldUtils.spawnParticleCubeOutline(deviceLocation.clone().add(-5.5, -5.5, -5.5), deviceLocation.clone().add(5.5, 5.5, 5.5), Particle.END_ROD, 5, true);
                        }
                    }

                    @Override
                    protected void end() {
                        bossBar.removeAll();
                        device.remove();
                        cooldown.setCooldown(info.getCooldown());
                        abilityActive.set(false);
                        nextFunction.run();
                    }
                }.runTaskRepeated(this, 0, 1, info.getDuration())
        ).execute();
    }
}
