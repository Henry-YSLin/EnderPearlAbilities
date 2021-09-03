package io.github.henryyslin.enderpearlabilities.pathfinder;

import io.github.henryyslin.enderpearlabilities.Ability;
import io.github.henryyslin.enderpearlabilities.AbilityCooldown;
import io.github.henryyslin.enderpearlabilities.ActivationHand;
import io.github.henryyslin.enderpearlabilities.utils.AdvancedRunnable;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

public class AbilityPathfinder implements Ability {
    static final int PROJECTILE_LIFETIME = 60;
    static final int GRAPPLE_LIFETIME = 100;

    public String getName() {
        return "Grappling Hook";
    }

    public String getOrigin() {
        return "Apex - Pathfinder";
    }

    public String getConfigName() {
        return "pathfinder";
    }

    public String getDescription() {
        return "Shoot a grappling hook to swing around.";
    }

    public ActivationHand getActivation() {
        return ActivationHand.OffHand;
    }

    public int getChargeUp() {
        return 0;
    }

    public int getDuration() {
        return GRAPPLE_LIFETIME;
    }

    public int getCooldown() {
        return 20;
    }

    final Plugin plugin;
    final FileConfiguration config;
    final String ownerName;
    final AtomicBoolean blockShoot = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    AbilityCooldown cooldown;
    AdvancedRunnable grapple;

    public AbilityPathfinder(Plugin plugin, FileConfiguration config) {
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
            if (grapple != null)
                if (!grapple.isCancelled())
                    grapple.cancel();
            cooldown = new AbilityCooldown(plugin, player);
            cooldown.startCooldown(getCooldown());
        }
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack item = event.getItem();

        if (!player.getName().equals(ownerName)) return;
        if (player.getInventory().getItemInOffHand().getType() != Material.ENDER_PEARL) return;
        if (item == null || item.getType() != Material.ENDER_PEARL) return;
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;

        if (abilityActive.get()) {
            grapple.cancel();
            return;
        }

        if (blockShoot.get()) return;
        blockShoot.set(true);

        if (player.getGameMode() != GameMode.CREATIVE) {
            player.getInventory().removeItem(new ItemStack(Material.ENDER_PEARL, 1));
        }

        Projectile projectile = player.launchProjectile(EnderPearl.class, player.getLocation().getDirection().clone().normalize().multiply(2.3));
        projectile.setGravity(false);

        projectile.setMetadata("ability", new FixedMetadataValue(plugin, ownerName));

        player.setCooldown(Material.ENDER_PEARL, 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (projectile.isValid()) {
                    projectile.remove();
                }
                blockShoot.set(false);
            }
        }.runTaskLater(plugin, PROJECTILE_LIFETIME);
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
        anchor.setMetadata("ability", new FixedMetadataValue(plugin, true));
        return anchor;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();

        if (!(shooter instanceof Player player)) return;
        if (!projectile.hasMetadata("ability")) return;
        if (!player.getName().equals(ownerName)) return;

        Entity hitEntity = event.getHitEntity();

        Mob anchor;

        if (hitEntity == null) {
            // improve accuracy of the hit location
            Location fixedLocation = projectile.getLocation();
            Vector offset = projectile.getVelocity().clone().normalize().multiply(0.5);
            int count = 0;

            World world = player.getWorld();
            while (!world.getBlockAt(fixedLocation).getType().isSolid() && count < 3) {
                fixedLocation.add(offset);
                count++;
            }
            anchor = spawnAnchor(player.getWorld(), fixedLocation);
        } else {
            anchor = spawnAnchor(player.getWorld(), hitEntity.getLocation());
        }

        (grapple = new AdvancedRunnable() {
            BossBar bossbar;
            Location anchorLocation;

            @Override
            protected void start() {
                abilityActive.set(true);
                blockShoot.set(false);
                bossbar = Bukkit.createBossBar(getName(), BarColor.PURPLE, BarStyle.SOLID);
                bossbar.addPlayer(player);
                anchorLocation = anchor.getLocation();
            }

            @Override
            protected void tick() {
                bossbar.setProgress(count / (double) getDuration());
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
                cooldown.startCooldown(getCooldown());
            }
        }).runTaskRepeated(plugin, 0, 1, GRAPPLE_LIFETIME);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        if (!abilityActive.get()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.getName().equals(ownerName)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!abilityActive.get()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onUnleash(EntityUnleashEvent event) {
        Entity entity = event.getEntity();
        if (!entity.hasMetadata("ability")) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                Collection<Entity> entities = entity.getWorld().getNearbyEntities(entity.getLocation(), 2, 2, 2, e -> e.getType() == EntityType.DROPPED_ITEM && ((Item) e).getItemStack().getType() == Material.LEAD);
                for (Entity entity : entities) {
                    entity.remove();
                }
            }
        }.runTaskLater(plugin, 1);
    }
}
