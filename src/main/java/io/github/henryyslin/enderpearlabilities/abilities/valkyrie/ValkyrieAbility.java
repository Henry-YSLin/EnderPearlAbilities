package io.github.henryyslin.enderpearlabilities.abilities.valkyrie;

import io.github.henryyslin.enderpearlabilities.abilities.Ability;
import io.github.henryyslin.enderpearlabilities.abilities.AbilityCouple;
import io.github.henryyslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henryyslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henryyslin.enderpearlabilities.utils.AbilityRunnable;
import io.github.henryyslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henryyslin.enderpearlabilities.utils.FunctionChain;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ValkyrieAbility extends Ability {

    static final int PROJECTILE_LIFETIME = 20;
    static final int ARROW_PER_TICK = 4;
    static final double PROJECTILE_SPEED = 5;

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 15);
        config.addDefault("cooldown", 400);
    }

    public ValkyrieAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("valkyrie")
                .name("Missile Swarm")
                .origin("Apex - Valkyrie")
                .description("Fire a swarm of missiles that damage and slow entities.")
                .usage("Right click to fire an ender pearl. Homing missiles will then be fired towards the hit position of the ender pearl. Entities hit by missiles will be slowed for a brief moment.")
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
    final Random random = new Random();
    final AtomicInteger enderPearlHitTime = new AtomicInteger();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        super.onPlayerJoin(event);
        Player player = event.getPlayer();
        if (player.getName().equals(ownerName)) {
            abilityActive.set(false);
            blockShoot.set(false);
            cooldown.startCooldown(info.cooldown);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (player != null) {
            abilityActive.set(false);
            blockShoot.set(false);
            cooldown.startCooldown(info.cooldown);
        }
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;

        new FunctionChain(
                next -> AbilityUtils.chargeUpSequence(this, player, info.chargeUp, next),
                next -> AbilityUtils.fireEnderPearl(this, player, blockShoot, PROJECTILE_LIFETIME, PROJECTILE_SPEED, false)
        ).execute();
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

        Entity hitEntity = event.getHitEntity();

        Location finalLocation;

        if (hitEntity == null) {
            // improve accuracy of the hit location
            finalLocation = AbilityUtils.correctProjectileHitLocation(projectile);
        } else {
            finalLocation = hitEntity.getLocation();
        }

        final List<Arrow> arrows = new ArrayList<>();

        new AbilityRunnable() {
            @Override
            public void tick() {
                if (arrows.size() == 0) return;
                boolean valid = false;
                for (Arrow arrow : arrows) {
                    if (arrow.isOnGround()) continue;
                    valid = true;

                    Optional<Object> boxedVector = AbilityUtils.getMetadata(arrow, "target");
                    if (boxedVector.isEmpty()) continue;
                    Vector target = (Vector) boxedVector.get();
                    Vector distance = target.clone().subtract(arrow.getLocation().toVector());
                    if (distance.lengthSquared() < 4) arrow.removeMetadata("target", plugin);

                    double propelStrength = 1;

                    Optional<Object> boxedHoming = AbilityUtils.getMetadata(arrow, "homing");
                    if (boxedHoming.isEmpty()) continue;
                    int homing = (int) boxedHoming.get();
                    arrow.removeMetadata("homing", plugin);
                    if (homing > 80) {
                        arrow.removeMetadata("target", plugin);
                    } else if (homing > 40) {
                        propelStrength = 1 + (homing - 40) / 40d;
                    } else {
                        arrow.setMetadata("homing", new FixedMetadataValue(plugin, homing + 1));
                    }

                    arrow.setVelocity(arrow.getVelocity().multiply(0.99).add(distance.normalize().multiply(propelStrength)).normalize().multiply(2));
                    arrow.getWorld().playSound(arrow.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.1f, 1 - homing / 80f);
                }
                if (!valid) cancel();
            }
        }.runTaskTimer(this, 0, 1);

        new FunctionChain(
                nextFunction -> new AbilityRunnable() {
                    @Override
                    protected void start() {
                        super.start();
                    }

                    @Override
                    protected void tick() {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.3f, 0);

                        Vector facing = player.getLocation().getDirection();
                        Vector offset = facing.clone().add(new Vector(0, 1, 0)).crossProduct(facing).normalize();
                        for (int i = 0; i < ARROW_PER_TICK; i++) {
                            Vector side = offset.clone();
                            if (i % 2 == 1)
                                side.multiply(-0.4);
                            else
                                side.multiply(0.4);
                            Arrow arrow = player.getWorld().spawn(player.getEyeLocation().add(side), Arrow.class, entity -> {
                                Vector velocity = facing.clone().multiply(4).add(new Vector(random.nextDouble() * 2d - 1d, random.nextDouble() * 2d + 2d, random.nextDouble() * 2d - 1d));
                                entity.setVelocity(velocity);
                                entity.setShooter(player);
                                entity.setTicksLived(1160);
                                entity.setCritical(true);
                                entity.setBounce(false);
                                entity.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                                entity.setBasePotionData(new PotionData(PotionType.SLOWNESS, false, true));
                                entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.codeName, ownerName)));
                                entity.setMetadata("homing", new FixedMetadataValue(plugin, 0));
                                entity.setMetadata("target", new FixedMetadataValue(plugin, finalLocation.toVector().add(new Vector(random.nextDouble() * 5d - 2.5d, random.nextDouble() * 5d - 2.5d, random.nextDouble() * 5d - 2.5d))));
                            });
                            arrows.add(arrow);
                        }
                    }

                    @Override
                    protected void end() {
                        super.end();
                        abilityActive.set(false);
                        if (this.hasCompleted())
                            cooldown.startCooldown(info.cooldown);
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
