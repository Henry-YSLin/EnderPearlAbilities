package io.github.henryyslin.enderpearlabilities.valkyrie;

import io.github.henryyslin.enderpearlabilities.Ability;
import io.github.henryyslin.enderpearlabilities.AbilityCooldown;
import io.github.henryyslin.enderpearlabilities.ActivationHand;
import io.github.henryyslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henryyslin.enderpearlabilities.utils.AdvancedRunnable;
import io.github.henryyslin.enderpearlabilities.utils.FunctionChain;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AbilityValkyrie implements Ability {
    static final int PROJECTILE_LIFETIME = 20;
    static final int ARROW_PER_TICK = 4;
    static final double PROJECTILE_SPEED = 4;

    public String getName() {
        return "Missile Swarm";
    }

    public String getOrigin() {
        return "Apex - Valkyrie";
    }

    public String getConfigName() {
        return "valkyrie";
    }

    public String getDescription() {
        return "Fire a swarm of missiles that damage and slow entities.";
    }

    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    public int getChargeUp() {
        return 0;
    }

    public int getDuration() {
        return 12;
    }

    public int getCooldown() {
        return 400;
    }

    final Plugin plugin;
    final FileConfiguration config;
    final String ownerName;
    final AtomicBoolean blockShoot = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    final Random random = new Random();
    AbilityCooldown cooldown;
    AtomicInteger enderPearlHitTime = new AtomicInteger();

    public AbilityValkyrie(Plugin plugin, FileConfiguration config) {
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

        AbilityUtils.relaunchEnderPearl(plugin, player, blockShoot, PROJECTILE_LIFETIME, PROJECTILE_SPEED);
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
        cooldown.startCooldown(getCooldown());
        abilityActive.set(true);
        blockShoot.set(false);
        enderPearlHitTime.set(player.getTicksLived());

        Entity hitEntity = event.getHitEntity();

        Location finalLocation;

        if (hitEntity == null) {
            // improve accuracy of the hit location
            finalLocation = AbilityUtils.fixProjectileHitLocation(player, projectile, PROJECTILE_SPEED);
        } else {
            finalLocation = hitEntity.getLocation();
        }

        final List<Arrow> arrows = new ArrayList<>();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (arrows.size() == 0) return;
                boolean valid = false;
                for (Arrow arrow : arrows) {
                    if (arrow.isOnGround()) continue;
                    valid = true;

                    if (!arrow.hasMetadata("target")) continue;
                    List<MetadataValue> metadataList = arrow.getMetadata("target");

                    if (metadataList.size() == 0) continue;
                    Vector target = (Vector) metadataList.get(0).value();
                    if (target == null) continue;
                    Vector distance = target.clone().subtract(arrow.getLocation().toVector());
                    if (distance.lengthSquared() < 4) arrow.removeMetadata("target", plugin);
                    arrow.setVelocity(arrow.getVelocity().multiply(0.99).add(distance.normalize().multiply(2)).normalize().multiply(2));
                }
                if (!valid) cancel();
            }
        }.runTaskTimer(plugin, 0, 1);

        new FunctionChain(
                nextFunction -> new BukkitRunnable() {
                    @Override
                    public void run() {
                        abilityActive.set(false);
                        nextFunction.invoke();
                    }
                }.runTaskLater(plugin, 1),
                nextFunction -> new AdvancedRunnable() {
                    @Override
                    protected void start() {
                        super.start();
                    }

                    @Override
                    protected void tick() {
                        Vector facing = player.getLocation().getDirection();
                        Vector offset = facing.clone().add(new Vector(0, 1, 0)).crossProduct(facing).normalize();
                        for (int i = 0; i < ARROW_PER_TICK; i++) {
                            Vector side = offset.clone();
                            if (i % 2 == 1)
                                side.multiply(-0.4);
                            else
                                side.multiply(0.4);
                            Arrow arrow = player.launchProjectile(Arrow.class, facing.clone().multiply(4).add(new Vector(random.nextDouble() * 2d - 1d, random.nextDouble() * 2d, random.nextDouble() * 2d - 1d)));

                            arrow.teleport(arrow.getLocation().add(side));
                            arrow.setTicksLived(1160);
                            arrow.setCritical(true);
                            arrow.setBounce(false);
                            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                            arrow.setBasePotionData(new PotionData(PotionType.SLOWNESS, false, true));
                            arrow.setMetadata("ability", new FixedMetadataValue(plugin, ownerName));
                            arrow.setMetadata("target", new FixedMetadataValue(plugin, finalLocation.toVector().add(new Vector(random.nextDouble() * 5d - 2.5d, random.nextDouble() * 5d - 2.5d, random.nextDouble() * 5d - 2.5d))));
                            arrows.add(arrow);
                        }
                    }

                    @Override
                    protected void end() {
                        super.end();
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
