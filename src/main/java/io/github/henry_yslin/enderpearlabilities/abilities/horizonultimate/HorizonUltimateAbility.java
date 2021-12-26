package io.github.henry_yslin.enderpearlabilities.abilities.horizonultimate;

import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.MathUtils;
import io.github.henry_yslin.enderpearlabilities.utils.WorldUtils;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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
                        world.playSound(finalLocation, Sound.ENTITY_BLAZE_AMBIENT, 1, 0);
                        world.spawnParticle(Particle.EXPLOSION_NORMAL, finalLocation, 2, 0.5, 0.5, 0.5, 0.02);
                    }

                    @Override
                    protected void end() {
                        chargingUp.set(false);
                        if (this.hasCompleted())
                            nextFunction.run();
                    }
                }.runTaskRepeated(this, 0, 2, info.getChargeUp() / 2),
                nextFunction -> new AbilityRunnable() {
                    Location blackHoleLocation;
                    BossBar bossBar;

                    @Override
                    protected void start() {
                        blackHoleLocation = finalLocation.clone();
                        world.playSound(blackHoleLocation, Sound.BLOCK_END_PORTAL_SPAWN, 1, 0);
                        bossBar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.getName(), BarColor.PURPLE, BarStyle.SOLID);
                        bossBar.addPlayer(player);
                    }

                    @Override
                    protected void tick() {
                        bossBar.setProgress((double) count / info.getDuration());
                        world.getNearbyEntities(blackHoleLocation, 5.5, 5.5, 5.5).forEach(entity -> {
                            if (entity instanceof Player player && player.getGameMode() == GameMode.SPECTATOR) return;
                            Vector velocity = MathUtils.clamp(entity.getVelocity(), 0.8);
                            velocity = MathUtils.replaceInfinite(velocity, new Vector(0, 0, 0));
                            Vector blackHole = blackHoleLocation.toVector().subtract(entity.getLocation().toVector());
                            double distance = blackHole.length();
                            blackHole.normalize().multiply(Math.min(1, 1.25 / distance));
                            blackHole = MathUtils.replaceInfinite(blackHole, new Vector(0, 0, 0));
                            entity.setVelocity(MathUtils.clamp(velocity.add(blackHole), 0.6));
                            if (count % 10 == 0)
                                if (entity instanceof LivingEntity livingEntity)
                                    livingEntity.damage(1, player);
                        });
                        //blackHoleLocation.add(0, 0.05, 0);
                        world.spawnParticle(Particle.SMOKE_LARGE, blackHoleLocation, 5, 0.5, 0.5, 0.5, 0.2);
                        if (count % 20 == 19) {
                            WorldUtils.spawnParticleCubeOutline(blackHoleLocation.clone().add(-5.5, -5.5, -5.5), blackHoleLocation.clone().add(5.5, 5.5, 5.5), Particle.END_ROD, 5, true);
                        }
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
