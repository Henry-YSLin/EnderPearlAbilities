package io.github.henryyslin.enderpearlabilities.horizon;

import io.github.henryyslin.enderpearlabilities.Ability;
import io.github.henryyslin.enderpearlabilities.AbilityCooldown;
import io.github.henryyslin.enderpearlabilities.ActivationHand;
import io.github.henryyslin.enderpearlabilities.utils.*;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
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

public class AbilityHorizon implements Ability {
    static final int PROJECTILE_LIFETIME = 100;
    static final double PROJECTILE_SPEED = 2;
    static final boolean PROJECTILE_GRAVITY = true;

    public String getName() {
        return "Black Hole";
    }

    public String getOrigin() {
        return "Apex - Horizon";
    }

    public String getConfigName() {
        return "horizon";
    }

    public String getDescription() {
        return "Create an inescapable micro black hole that pulls all entities in towards it.";
    }

    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    public int getChargeUp() {
        return 20;
    }

    public int getDuration() {
        return 200;
    }

    public int getCooldown() {
        return 1000;
    }

    final Plugin plugin;
    final FileConfiguration config;
    final String ownerName;
    final AtomicBoolean blockShoot = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    AbilityCooldown cooldown;
    AtomicInteger enderPearlHitTime = new AtomicInteger();

    public AbilityHorizon(Plugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
        this.ownerName = config.getString(getConfigName());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getName().equals(ownerName)) {
            abilityActive.set(false);
            blockShoot.set(false);
            cooldown = new AbilityCooldown(plugin, player);
            cooldown.startCooldown(getCooldown());
        }
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, getActivation())) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;
        if (abilityActive.get()) return;

        AbilityUtils.relaunchEnderPearl(plugin, player, blockShoot, PROJECTILE_LIFETIME, PROJECTILE_SPEED, PROJECTILE_GRAVITY);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();

        if (!(shooter instanceof Player player)) return;
        if (!projectile.hasMetadata("ability")) return;
        if (!player.getName().equals(ownerName)) return;

        if (!(projectile instanceof EnderPearl)) return;

        event.setCancelled(true);

        projectile.remove();
        abilityActive.set(true);
        blockShoot.set(false);
        enderPearlHitTime.set(player.getTicksLived());

        Location finalLocation = projectile.getLocation();

        WorldUtils.spawnParticleRect(finalLocation.clone().add(-5.5, -5.5, -5.5), finalLocation.clone().add(5.5, 5.5, 5.5), Particle.VILLAGER_HAPPY, 5);

        World world = projectile.getWorld();

        new FunctionChain(
                nextFunction -> new AdvancedRunnable() {
                    @Override
                    protected void tick() {
                        world.spawnParticle(Particle.EXPLOSION_NORMAL, finalLocation, 2, 0.5, 0.5, 0.5, 0.02);
                    }

                    @Override
                    protected void end() {
                        nextFunction.invoke();
                    }
                }.runTaskRepeated(plugin, 0, 2, getChargeUp() / 2),
                nextFunction -> new AdvancedRunnable() {
                    Location blackHoleLocation;

                    @Override
                    protected void start() {
                        blackHoleLocation = finalLocation.clone();
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
                                    livingEntity.damage(0.7);
                        });
                        blackHoleLocation.add(0, 0.05, 0);
                        world.spawnParticle(Particle.SMOKE_LARGE, blackHoleLocation, 5, 0.5, 0.5, 0.5, 0.2);
                        if (count % 20 == 0) {
                            WorldUtils.spawnParticleRect(blackHoleLocation.clone().add(-5.5, -5.5, -5.5), blackHoleLocation.clone().add(5.5, 5.5, 5.5), Particle.VILLAGER_HAPPY, 5);
                        }
                    }

                    @Override
                    protected void end() {
                        cooldown.startCooldown(getCooldown());
                        abilityActive.set(false);
                        nextFunction.invoke();
                    }
                }.runTaskRepeated(plugin, 0, 1, getDuration())
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
