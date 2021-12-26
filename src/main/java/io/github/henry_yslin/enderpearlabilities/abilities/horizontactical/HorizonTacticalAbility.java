package io.github.henry_yslin.enderpearlabilities.abilities.horizontactical;

import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.MathUtils;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class HorizonTacticalAbility extends Ability<HorizonTacticalAbilityInfo> {

    static final int PROJECTILE_LIFETIME = 50;
    static final double PROJECTILE_SPEED = 2;
    static final boolean PROJECTILE_GRAVITY = true;
    static final double GRAVITY_LIFT_HEIGHT = 20;

    public HorizonTacticalAbility(Plugin plugin, HorizonTacticalAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean blockShoot = new AtomicBoolean(false);
    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    SpacewalkRunnable spacewalkRunnable;

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
        if (spacewalkRunnable != null && !spacewalkRunnable.isCancelled())
            spacewalkRunnable.cancel();
        spacewalkRunnable = new SpacewalkRunnable(player);
        spacewalkRunnable.runTaskTimer(this, 0, 1);
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;

        event.setCancelled(true);

        if (cooldown.isCoolingDown()) return;
        if (abilityActive.get()) return;

        AbilityUtils.consumeEnderPearl(this, player);
        AbilityUtils.fireProjectile(this, player, blockShoot, PROJECTILE_LIFETIME, PROJECTILE_SPEED, PROJECTILE_GRAVITY);
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

        projectile.remove();
        abilityActive.set(true);
        blockShoot.set(false);

        Location finalLocation = projectile.getLocation();

        World world = projectile.getWorld();

        new FunctionChain(
                nextFunction -> new AbilityRunnable() {
                    @Override
                    protected void start() {
                        chargingUp.set(true);
                    }

                    @Override
                    protected void tick() {
                        world.spawnParticle(Particle.EXPLOSION_NORMAL, finalLocation, 2, 0.1, 0.1, 0.1, 0.01);
                    }

                    @Override
                    protected void end() {
                        chargingUp.set(false);
                        if (this.hasCompleted())
                            nextFunction.run();
                    }
                }.runTaskRepeated(this, 0, 2, info.getChargeUp() / 2),
                nextFunction -> new AbilityRunnable() {
                    BossBar bossBar;
                    List<Entity> entities;

                    @Override
                    protected void start() {
                        bossBar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.getName(), BarColor.PURPLE, BarStyle.SOLID);
                        bossBar.addPlayer(player);
                    }

                    @Override
                    protected void tick() {
                        bossBar.setProgress((double) count / info.getDuration());
                        List<Entity> newEntities = world.getNearbyEntities(finalLocation.clone().add(0, GRAVITY_LIFT_HEIGHT / 2, 0), 1.5, GRAVITY_LIFT_HEIGHT, 1.5).stream().toList();
                        if (entities != null)
                            for (Entity entity : entities) {
                                if (!newEntities.contains(entity)) {
                                    Location pastLoc = entity.getLocation();
                                    new AbilityRunnable() {
                                        @Override
                                        protected void tick() {
                                            entity.setVelocity(entity.getVelocity().add(MathUtils.clamp(entity.getLocation().subtract(pastLoc).toVector().setY(0).multiply(10), 2)));
                                        }
                                    }.runTaskLater(executor, 1);
                                }
                            }
                        entities = newEntities;
                        entities.forEach(entity -> {
                            if (entity instanceof Player player && player.getGameMode() == GameMode.SPECTATOR) return;
                            Vector verticalVelocity = entity.getVelocity().add(new Vector(0, 0.1, 0));
                            entity.setVelocity(verticalVelocity.setY(Math.min(verticalVelocity.getY(), 0.7)));
                            if (entity.getVelocity().getY() > 0)
                                if (entity instanceof LivingEntity livingEntity) {
                                    if (livingEntity.hasMetadata("gravity-lift")) return;
                                    new GravityLiftRunnable(livingEntity).runTaskTimer(executor, 0, 1);
                                }
                        });
                        for (int i = 0; i < 5; i++)
                            world.spawnParticle(Particle.SMOKE_NORMAL, finalLocation.clone().add(Math.random() * 3 - 1.5, Math.random() * GRAVITY_LIFT_HEIGHT, Math.random() * 3 - 1.5), 0, 0, 0.5, 0, 1);
                        for (int i = 0; i < 5; i++)
                            world.spawnParticle(Particle.SMOKE_NORMAL, finalLocation.clone().add(Math.random() * 3 - 1.5, 0, Math.random() * 3 - 1.5), 0, 0, 0.5, 0, 1);
                        if (count % 10 == 9 && count > 20)
                            world.playSound(finalLocation, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 1);
                    }

                    @Override
                    protected void end() {
                        bossBar.removeAll();
                        if (this.hasCompleted())
                            cooldown.setCooldown(info.getCooldown());
                        abilityActive.set(false);
                        nextFunction.run();
                    }
                }.runTaskRepeated(this, 0, 1, info.getDuration())
        ).execute();
    }
}
