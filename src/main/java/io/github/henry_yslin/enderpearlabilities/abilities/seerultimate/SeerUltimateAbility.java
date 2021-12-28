package io.github.henry_yslin.enderpearlabilities.abilities.seerultimate;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityCouple;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.ProjectileUtils;
import io.github.henry_yslin.enderpearlabilities.utils.WorldUtils;
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
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.concurrent.atomic.AtomicBoolean;

public class SeerUltimateAbility extends Ability<SeerUltimateAbilityInfo> {

    static final int PROJECTILE_LIFETIME = 50;
    static final double PROJECTILE_SPEED = 1;
    static final boolean PROJECTILE_GRAVITY = true;
    static final double DETECTION_RADIUS = 45;

    public SeerUltimateAbility(Plugin plugin, SeerUltimateAbilityInfo info, String ownerName) {
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
    public void onEntityDeath(EntityDeathEvent event) {
        if (!AbilityUtils.verifyAbilityCouple(this, event.getEntity())) return;
        event.getDrops().clear();
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;

        event.setCancelled(true);

        if (cooldown.isCoolingDown()) return;
        if (abilityActive.get()) return;

        AbilityUtils.consumeEnderPearl(this, player);
        EnderPearlAbilities.getInstance().emitEvent(
                EventListener.class,
                new AbilityActivateEvent(this),
                EventListener::onAbilityActivate
        );
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
        blockShoot.set(false);

        Location finalLocation = ProjectileUtils.correctProjectileHitLocation(projectile);

        Shulker chamber = projectile.getWorld().spawn(finalLocation, Shulker.class, entity -> {
            entity.setAI(false);
            entity.setSilent(true);
            entity.setPeek(0);
            entity.setColor(DyeColor.MAGENTA);
            entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.getCodeName(), ownerName)));
        });

        World world = projectile.getWorld();

        new FunctionChain(
                nextFunction -> new AbilityRunnable() {
                    BossBar bossBar;

                    @Override
                    protected void start() {
                        chargingUp.set(true);
                        bossBar = Bukkit.createBossBar("Charging up", BarColor.WHITE, BarStyle.SOLID);
                        bossBar.addPlayer(player);
                    }

                    @Override
                    protected void tick() {
                        if (!chamber.isValid()) {
                            cancel();
                            return;
                        }
                        bossBar.setProgress(1 - (double) count / info.getChargeUp() * 2);
                        chamber.getWorld().playSound(chamber.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.05f, 0.3f);
                        chamber.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, chamber.getLocation(), 2, 0.5, 0.5, 0.5, 0.05);
                    }

                    @Override
                    protected void end() {
                        bossBar.removeAll();
                        chargingUp.set(false);
                        if (this.hasCompleted())
                            nextFunction.run();
                    }
                }.runTaskRepeated(this, 0, 2, info.getChargeUp() / 2),
                nextFunction -> new AbilityRunnable() {
                    BossBar bossBar;

                    @Override
                    protected void start() {
                        abilityActive.set(true);
                        bossBar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.getName(), BarColor.PURPLE, BarStyle.SOLID);
                        bossBar.addPlayer(player);
                        chamber.getWorld().playSound(chamber.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, 1, 2);
                        chamber.getWorld().spawnParticle(Particle.END_ROD, chamber.getLocation(), 1000, 1, 1, 1, 1, null, true);
                    }

                    @Override
                    protected void tick() {
                        if (!chamber.isValid()) {
                            cancel();
                            return;
                        }
                        bossBar.setProgress((double) count / info.getDuration());
                        world.getNearbyEntities(chamber.getLocation(), DETECTION_RADIUS, DETECTION_RADIUS, DETECTION_RADIUS).forEach(entity -> {
                            if (entity instanceof Player player && player.getGameMode() == GameMode.SPECTATOR) return;
                            if (entity.getLocation().distanceSquared(chamber.getLocation()) > DETECTION_RADIUS * DETECTION_RADIUS)
                                return;
                            if (entity instanceof LivingEntity livingEntity) {
                                if (entity instanceof Player player) {
                                    if (player.isSneaking()) return;
                                    if (player.getName().equals(ownerName)) return;
                                }
                                livingEntity.addPotionEffect(PotionEffectType.GLOWING.createEffect(5, 0));
                            }
                        });
                        if (count > 20)
                            chamber.getWorld().playSound(chamber.getLocation(), Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 0.02f, 2);
                        chamber.getWorld().spawnParticle(Particle.END_ROD, chamber.getLocation(), 2, 0.5, 0.5, 0.5, 0.05);
                        WorldUtils.spawnParticleSphere(chamber.getLocation(), DETECTION_RADIUS, Particle.END_ROD, 100, true);
                    }

                    @Override
                    protected void end() {
                        bossBar.removeAll();
                        chamber.remove();
                        if (this.hasCompleted())
                            cooldown.setCooldown(info.getCooldown());
                        abilityActive.set(false);
                        nextFunction.run();
                    }
                }.runTaskRepeated(this, 0, 1, info.getDuration())
        ).execute();
    }
}
