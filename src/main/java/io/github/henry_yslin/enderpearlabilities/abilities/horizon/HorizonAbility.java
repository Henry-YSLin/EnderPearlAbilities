package io.github.henry_yslin.enderpearlabilities.abilities.horizon;

import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henry_yslin.enderpearlabilities.utils.*;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class HorizonAbility extends Ability {

    static final int PROJECTILE_LIFETIME = 100;
    static final double PROJECTILE_SPEED = 2;
    static final boolean PROJECTILE_GRAVITY = true;

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 20);
        config.addDefault("duration", 200);
        config.addDefault("cooldown", 1000);
    }

    public HorizonAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("horizon")
                .name("Black Hole")
                .origin("Apex - Horizon")
                .description("Create an inescapable micro black hole that pulls all entities in towards it.\nPassive ability: Cushion your fall to reduce fall damage.")
                .usage("Right click to throw an ender pearl. A black hole will be created where the pearl lands.")
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

    final AtomicBoolean blockShoot = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    final AtomicInteger enderPearlHitTime = new AtomicInteger();
    SpacewalkRunnable spacewalkRunnable;

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
        cooldown.startCooldown(info.cooldown);
        if (spacewalkRunnable != null && !spacewalkRunnable.isCancelled())
            spacewalkRunnable.cancel();
        spacewalkRunnable = new SpacewalkRunnable(player);
        spacewalkRunnable.runTaskTimer(this, 0, 1);
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;
        if (abilityActive.get()) return;

        PlayerUtils.consumeEnderPearl(player);
        AbilityUtils.fireEnderPearl(this, player, blockShoot, PROJECTILE_LIFETIME, PROJECTILE_SPEED, PROJECTILE_GRAVITY);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();

        if (!(shooter instanceof Player player)) return;
        if (!AbilityUtils.verifyAbilityCouple(this, projectile)) return;
        if (!player.getName().equals(ownerName)) return;

        if (!(projectile instanceof EnderPearl)) return;

        event.setCancelled(true);

        projectile.remove();
        abilityActive.set(true);
        blockShoot.set(false);
        enderPearlHitTime.set(player.getTicksLived());

        Location finalLocation = projectile.getLocation();

        WorldUtils.spawnParticleRect(finalLocation.clone().add(-5.5, -5.5, -5.5), finalLocation.clone().add(5.5, 5.5, 5.5), Particle.VILLAGER_HAPPY, 5, true);

        World world = projectile.getWorld();

        new FunctionChain(
                nextFunction -> new AbilityRunnable() {
                    @Override
                    protected void tick() {
                        world.playSound(finalLocation, Sound.ENTITY_BLAZE_AMBIENT, 1, 0);
                        world.spawnParticle(Particle.EXPLOSION_NORMAL, finalLocation, 2, 0.5, 0.5, 0.5, 0.02);
                    }

                    @Override
                    protected void end() {
                        if (this.hasCompleted())
                            nextFunction.run();
                    }
                }.runTaskRepeated(this, 0, 2, info.chargeUp / 2),
                nextFunction -> new AbilityRunnable() {
                    Location blackHoleLocation;

                    @Override
                    protected void start() {
                        blackHoleLocation = finalLocation.clone();
                        world.playSound(blackHoleLocation, Sound.BLOCK_END_PORTAL_SPAWN, 1, 0);
                    }

                    @Override
                    protected void tick() {
                        world.getNearbyEntities(blackHoleLocation, 5.5, 5.5, 5.5).forEach(entity -> {
                            if (entity instanceof Player player && player.getGameMode() == GameMode.SPECTATOR) return;
                            Vector velocity = MathUtils.clamp(entity.getVelocity(), 0.1);
                            velocity = MathUtils.replaceInfinite(velocity, new Vector(0, 0, 0));
                            Vector blackHole = blackHoleLocation.toVector().subtract(entity.getLocation().toVector());
                            double distance = blackHole.length();
                            blackHole.normalize().multiply(Math.min(1, 2 / distance));
                            blackHole = MathUtils.replaceInfinite(blackHole, new Vector(0, 0, 0));
                            entity.setVelocity(MathUtils.clamp(velocity.add(blackHole), 0.6));
                            if (count % 10 == 0)
                                if (entity instanceof LivingEntity livingEntity)
                                    livingEntity.damage(0.7, player);
                        });
                        blackHoleLocation.add(0, 0.05, 0);
                        world.spawnParticle(Particle.SMOKE_LARGE, blackHoleLocation, 5, 0.5, 0.5, 0.5, 0.2);
                        if (count % 20 == 0) {
                            WorldUtils.spawnParticleRect(blackHoleLocation.clone().add(-5.5, -5.5, -5.5), blackHoleLocation.clone().add(5.5, 5.5, 5.5), Particle.VILLAGER_HAPPY, 5, true);
                        }
                    }

                    @Override
                    protected void end() {
                        if (this.hasCompleted())
                            cooldown.startCooldown(info.cooldown);
                        abilityActive.set(false);
                        nextFunction.run();
                    }
                }.runTaskRepeated(this, 0, 1, info.duration)
        ).execute();
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        if (Math.abs(event.getPlayer().getTicksLived() - enderPearlHitTime.get()) > 1) return;
        event.setCancelled(true);
    }
}
