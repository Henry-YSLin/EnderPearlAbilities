package io.github.henryyslin.enderpearlabilities.abilities.valkyrie;

import io.github.henryyslin.enderpearlabilities.abilities.Ability;
import io.github.henryyslin.enderpearlabilities.abilities.AbilityCouple;
import io.github.henryyslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henryyslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henryyslin.enderpearlabilities.utils.*;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class ValkyrieAbility extends Ability {

    static final int TARGET_RANGE = 100;
    static final int ARROW_PER_TICK = 4;

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
                .usage("Right click to fire homing missiles towards your crosshair location. Entities hit by missiles will be slowed for a brief moment.")
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

    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    final Random random = new Random();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        super.onPlayerJoin(event);
        Player player = event.getPlayer();
        if (player.getName().equals(ownerName)) {
            abilityActive.set(false);
            cooldown.startCooldown(info.cooldown);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (player != null) {
            abilityActive.set(false);
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
                next -> {
                    RayTraceResult result = player.getWorld().rayTrace(player.getEyeLocation(), player.getEyeLocation().getDirection(), TARGET_RANGE, FluidCollisionMode.NEVER, true, 0, entity -> !entity.equals(player));
                    Location targetLocation;
                    if (result == null)
                        targetLocation = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(TARGET_RANGE));
                    else
                        targetLocation = result.getHitPosition().toLocation(player.getWorld());
                    WorldUtils.spawnParticleRect(targetLocation.clone().add(-2.5, -2.5, -2.5), targetLocation.clone().add(2.5, 2.5, 2.5), Particle.SMOKE_NORMAL, 5, true);
                    fireMissiles(targetLocation);
                }
        ).execute();
    }

    private void fireMissiles(Location targetLocation) {
        final List<Arrow> arrows = new ArrayList<>();

        new AbilityRunnable() {
            @Override
            public void tick() {
                if (arrows.size() == 0) return;
                boolean valid = false;
                for (Arrow arrow : arrows) {
                    if (arrow.isOnGround()) continue;
                    if (!arrow.isValid()) continue;
                    valid = true;

                    Optional<Object> boxedVector = EntityUtils.getMetadata(arrow, "target");
                    if (boxedVector.isEmpty()) continue;
                    Vector target = (Vector) boxedVector.get();
                    Vector distance = target.clone().subtract(arrow.getLocation().toVector());
                    if (distance.lengthSquared() < 4) arrow.removeMetadata("target", plugin);

                    double propelStrength = 1;

                    Optional<Object> boxedHoming = EntityUtils.getMetadata(arrow, "homing");
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

        new AbilityRunnable() {
            @Override
            protected void start() {
                super.start();
                abilityActive.set(true);
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
                        entity.setMetadata("target", new FixedMetadataValue(plugin, targetLocation.toVector().add(new Vector(random.nextDouble() * 5d - 2.5d, random.nextDouble() * 5d - 2.5d, random.nextDouble() * 5d - 2.5d))));
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
            }
        }.runTaskRepeated(this, 0, 1, info.duration);
    }
}
