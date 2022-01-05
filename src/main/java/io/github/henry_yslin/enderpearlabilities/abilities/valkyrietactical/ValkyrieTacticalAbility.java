package io.github.henry_yslin.enderpearlabilities.abilities.valkyrietactical;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityCouple;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.EntityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.WorldUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
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
import java.util.concurrent.atomic.AtomicReference;

public class ValkyrieTacticalAbility extends Ability<ValkyrieTacticalAbilityInfo> {

    static final int TARGET_RANGE = 100;
    static final int ARROW_PER_TICK = 4;

    public ValkyrieTacticalAbility(Plugin plugin, ValkyrieTacticalAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    final Random random = new Random();
    VTOLJetsRunnable vtolJetsRunnable;

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
        chargingUp.set(false);
        abilityActive.set(false);
        cooldown.setCooldown(info.getCooldown());

        if (vtolJetsRunnable != null && !vtolJetsRunnable.isCancelled())
            vtolJetsRunnable.cancel();
        vtolJetsRunnable = new VTOLJetsRunnable(player);
        vtolJetsRunnable.runTaskTimer(this, 0, 1);
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;

        event.setCancelled(true);

        if (cooldown.isCoolingDown()) return;
        if (chargingUp.get()) return;
        if (abilityActive.get()) return;

        AtomicReference<Location> targetLocation = new AtomicReference<>();
        new FunctionChain(
                next -> {
                    RayTraceResult result = player.getWorld().rayTrace(player.getEyeLocation(), player.getEyeLocation().getDirection(), TARGET_RANGE, FluidCollisionMode.NEVER, true, 0, entity -> !entity.equals(player));
                    if (result == null) {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Too far away"));
                        return;
                    } else
                        targetLocation.set(result.getHitPosition().toLocation(player.getWorld()));
                    AbilityUtils.consumeEnderPearl(this, player);
                    EnderPearlAbilities.getInstance().emitEvent(
                            EventListener.class,
                            new AbilityActivateEvent(this),
                            EventListener::onAbilityActivate
                    );
                    WorldUtils.spawnParticleCubeOutline(targetLocation.get().clone().add(-2.5, -2.5, -2.5), targetLocation.get().clone().add(2.5, 2.5, 2.5), Particle.SMOKE_NORMAL, 5, true);
                    next.run();
                },
                next -> AbilityUtils.chargeUpSequence(this, player, info.getChargeUp(), chargingUp, next),
                next -> fireMissiles(targetLocation.get())
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
                if (!player.isValid()) {
                    cancel();
                    return;
                }
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
                        entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.getCodeName(), ownerName)));
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
                cooldown.setCooldown(info.getCooldown());
            }
        }.runTaskRepeated(this, 0, 1, info.getDuration());
    }
}
