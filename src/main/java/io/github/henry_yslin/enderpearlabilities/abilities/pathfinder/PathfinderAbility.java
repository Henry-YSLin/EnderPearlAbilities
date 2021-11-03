package io.github.henry_yslin.enderpearlabilities.abilities.pathfinder;

import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityCouple;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henry_yslin.enderpearlabilities.utils.*;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PathfinderAbility extends Ability {

    static final int PROJECTILE_LIFETIME = 20;
    static final double PROJECTILE_SPEED = 4;

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 100);
        config.addDefault("cooldown", 20);
    }

    public PathfinderAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("pathfinder")
                .name("Grappling Hook")
                .origin("Apex - Pathfinder")
                .description("Shoot a grappling hook to swing around, pull yourself up, or pull other entities close to you.\nPassive ability: Break your fall for a brief moment if the fall will be lethal.")
                .usage("Right click to throw an ender pearl. The grapple anchors to where the ender pearl hits. Look at the anchor while grappling to pull yourself towards the anchor. Look sideways to swing. Right click again to cancel the grapple.")
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
    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    AbilityRunnable grapple;
    final AtomicInteger enderPearlHitTime = new AtomicInteger();
    SlowFallRunnable slowFallRunnable;

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
        chargingUp.set(false);
        abilityActive.set(false);
        blockShoot.set(false);
        if (grapple != null)
            if (!grapple.isCancelled())
                grapple.cancel();
        cooldown.startCooldown(info.cooldown);
        if (slowFallRunnable != null && !slowFallRunnable.isCancelled())
            slowFallRunnable.cancel();
        slowFallRunnable = new SlowFallRunnable(player);
        slowFallRunnable.runTaskTimer(this, 0, 5);
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

        PlayerUtils.consumeEnderPearl(player);

        new FunctionChain(
                next -> AbilityUtils.chargeUpSequence(this, player, info.chargeUp, chargingUp, next),
                next -> AbilityUtils.fireEnderPearl(this, player, blockShoot, PROJECTILE_LIFETIME, PROJECTILE_SPEED, false)
        ).execute();
    }

    @EventHandler
    public synchronized void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        if (!abilityActive.get()) return;

        grapple.cancel();
    }

    private Mob spawnAnchor(World world, Location location) {
        return world.spawn(location, Slime.class, false, entity -> {
            entity.setAI(false);
            entity.setSilent(true);
            entity.setAware(false);
            entity.setCollidable(false);
            entity.setGravity(false);
            entity.setInvulnerable(true);
            entity.setInvisible(true);
            entity.setSize(0);
            entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.codeName, ownerName)));
        });
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.getName().equals(ownerName)) return;
        if (!abilityActive.get()) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL)
            event.setCancelled(true);
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
            Location fixedLocation = ProjectileUtils.correctProjectileHitLocation(projectile);
            anchor = spawnAnchor(player.getWorld(), fixedLocation);
        } else {
            anchor = spawnAnchor(player.getWorld(), hitEntity.getLocation());
        }

        anchor.getWorld().playSound(anchor.getLocation(), Sound.BLOCK_CHAIN_FALL, 1, 0);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_FALL, 0.3f, 0);

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
                if (!player.isValid()) {
                    cancel();
                    return;
                }
                bossbar.setProgress(count / (double) info.duration);
                if (hitEntity != null)
                    anchorLocation = hitEntity.getLocation();
                anchor.teleport(anchorLocation);
                anchor.setLeashHolder(player);

                if (!Objects.equals(anchorLocation.getWorld(), player.getWorld())) {
                    cancel();
                    return;
                }

                Vector lookDirection = player.getLocation().getDirection().normalize();

                player.setVelocity(player.getVelocity().add(lookDirection.clone().multiply(Math.max(0.000001, Math.min(0.1, player.getVelocity().length())))));

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
                if (finalVelocity.length() > 0)
                    player.setVelocity(finalVelocity.normalize().multiply(magnitude));

                if (idealForce > 0.01)
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LEASH_KNOT_PLACE, 0.1f, 1.2f);
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
        AbilityUtils.delay(this, 1, () -> {
            Collection<Entity> entities = entity.getWorld().getNearbyEntities(entity.getLocation(), 2, 2, 2, e -> e.getType() == EntityType.DROPPED_ITEM && ((Item) e).getItemStack().getType() == Material.LEAD);
            for (Entity e : entities) {
                e.remove();
            }
        }, true);
    }
}
