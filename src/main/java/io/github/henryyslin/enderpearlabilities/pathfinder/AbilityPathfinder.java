package io.github.henryyslin.enderpearlabilities.pathfinder;

import io.github.henryyslin.enderpearlabilities.*;
import io.github.henryyslin.enderpearlabilities.utils.AbilityRunnable;
import io.github.henryyslin.enderpearlabilities.utils.AbilityUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AbilityPathfinder extends Ability {
    static final int PROJECTILE_LIFETIME = 20;
    static final double PROJECTILE_SPEED = 4;

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        // TODO: add charge up
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 100);
        config.addDefault("cooldown", 20);
    }

    public AbilityPathfinder(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("pathfinder")
                .name("Grappling Hook")
                .origin("Apex - Pathfinder")
                .description("Shoot a grappling hook to swing around, pull yourself up, or pull other entities close to you.")
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

    final AtomicBoolean blockShoot = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    AbilityCooldown cooldown;
    AbilityRunnable grapple;
    final AtomicInteger enderPearlHitTime = new AtomicInteger();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getName().equals(ownerName)) {
            abilityActive.set(false);
            blockShoot.set(false);
            if (grapple != null)
                if (!grapple.isCancelled())
                    grapple.cancel();
            cooldown = new AbilityCooldown(this, player);
            cooldown.startCooldown(info.cooldown);
        }
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;

        if (abilityActive.get()) {
            grapple.cancel();
            return;
        }

        AbilityUtils.relaunchEnderPearl(this, player, blockShoot, PROJECTILE_LIFETIME, PROJECTILE_SPEED);
    }

    @EventHandler
    public synchronized void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        if (!abilityActive.get()) return;

        grapple.cancel();
    }

    private Mob spawnAnchor(World world, Location location) {
        Mob anchor = (Mob) world.spawnEntity(location, EntityType.SQUID);
        anchor.setAI(false);
        anchor.setGravity(false);
        anchor.setInvulnerable(true);
        anchor.setInvisible(true);
        anchor.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.codeName, ownerName)));
        return anchor;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();

        if (!(shooter instanceof Player player)) return;
        if (!AbilityUtils.verifyAbilityCouple(this, projectile)) return;
        if (!player.getName().equals(ownerName)) return;

        event.setCancelled(true);
        projectile.remove();
        enderPearlHitTime.set(player.getTicksLived());

        Entity hitEntity = event.getHitEntity();
        Mob anchor;

        if (hitEntity == null) {
            // improve accuracy of the hit location
            Location fixedLocation = AbilityUtils.fixProjectileHitLocation(player, projectile, PROJECTILE_SPEED);
            anchor = spawnAnchor(player.getWorld(), fixedLocation);
        } else {
            anchor = spawnAnchor(player.getWorld(), hitEntity.getLocation());
        }

        (grapple = new AbilityRunnable() {
            BossBar bossbar;
            Location anchorLocation;

            @Override
            protected void start() {
                abilityActive.set(true);
                blockShoot.set(false);
                bossbar = Bukkit.createBossBar(info.name, BarColor.PURPLE, BarStyle.SOLID);
                bossbar.addPlayer(player);
                anchorLocation = anchor.getLocation();
            }

            @Override
            protected void tick() {
                bossbar.setProgress(count / (double) info.duration);
                if (hitEntity != null)
                    anchorLocation = hitEntity.getLocation();
                anchor.teleport(anchorLocation);
                anchor.setLeashHolder(player);

                Vector lookDirection = player.getLocation().getDirection().normalize();

                player.setVelocity(player.getVelocity().add(lookDirection.clone().multiply(0.3)));

                Vector distance = anchor.getLocation().subtract(player.getLocation()).toVector();
                if (distance.lengthSquared() < 3) {
                    cancel();
                }
                Vector grapple = distance.normalize();

                if (hitEntity != null) {
                    Vector entityVelocity = hitEntity.getVelocity().add(grapple.clone().multiply(-1));
                    double magnitude = Math.min(1, entityVelocity.length());
                    hitEntity.setVelocity(entityVelocity.normalize().multiply(magnitude));
                }

                double idealForce = Math.max(0, grapple.dot(lookDirection));

                grapple.multiply(Math.max(0, idealForce - grapple.dot(player.getVelocity())));

                Vector finalVelocity = player.getVelocity().add(grapple);
                double magnitude = Math.min(1, finalVelocity.length());
                player.setVelocity(finalVelocity.normalize().multiply(magnitude));
            }

            @Override
            protected void end() {
                bossbar.removeAll();
                anchor.remove();
                abilityActive.set(false);
                cooldown.startCooldown(info.cooldown);
            }
        }).runTaskRepeated(this, 0, 1, info.duration);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        if (Math.abs(event.getPlayer().getTicksLived() - enderPearlHitTime.get()) > 1) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onUnleash(EntityUnleashEvent event) {
        Entity entity = event.getEntity();
        if (!AbilityUtils.verifyAbilityCouple(this, entity)) return;
        new AbilityRunnable() {
            @Override
            public void tick() {
                Collection<Entity> entities = entity.getWorld().getNearbyEntities(entity.getLocation(), 2, 2, 2, e -> e.getType() == EntityType.DROPPED_ITEM && ((Item) e).getItemStack().getType() == Material.LEAD);
                for (Entity entity : entities) {
                    entity.remove();
                }
            }
        }.runTaskLater(this, 1);
    }
}
