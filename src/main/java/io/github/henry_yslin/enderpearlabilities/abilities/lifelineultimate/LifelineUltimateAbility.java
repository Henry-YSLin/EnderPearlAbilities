package io.github.henry_yslin.enderpearlabilities.abilities.lifelineultimate;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityCouple;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.WorldUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class LifelineUltimateAbility extends Ability<LifelineUltimateAbilityInfo> {

    public LifelineUltimateAbility(Plugin plugin, LifelineUltimateAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    final AtomicBoolean chargingUp = new AtomicBoolean(false);

    @Override
    public boolean isActive() {
        return abilityActive.get();
    }

    @Override
    public boolean isChargingUp() {
        return chargingUp.get();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (player != null) {
            abilityActive.set(false);
            cooldown.setCooldown(info.getCooldown());
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        super.onPlayerJoin(event);
        Player player = event.getPlayer();
        if (player.getName().equals(ownerName)) {
            abilityActive.set(false);
            cooldown.setCooldown(info.getCooldown());
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!AbilityUtils.verifyAbilityCouple(this, event.getRightClicked())) return;
        if (!abilityActive.get()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;

        event.setCancelled(true);

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        if (cooldown.isCoolingDown()) return;
        if (chargingUp.get()) return;
        if (abilityActive.get()) return;

        World world = player.getWorld();

        if (!event.getClickedBlock().getType().isSolid()) return;
        Location location = event.getClickedBlock().getLocation();
        location.add(0.5, 1.5, 0.5);
        if (location.getBlock().getType() != Material.AIR) return;
        WorldUtils.spawnParticleCubeOutline(location.clone().add(-0.5, -0.5, -0.5), location.clone().add(0.5, 0.5, 0.5), Particle.END_ROD, 5, true);

        new FunctionChain(
                next -> AbilityUtils.chargeUpSequence(this, player, info.getChargeUp(), chargingUp, next),
                next -> {
                    AbilityUtils.consumeEnderPearl(this, player);
                    EnderPearlAbilities.getInstance().emitEvent(
                            EventListener.class,
                            new AbilityActivateEvent(this),
                            EventListener::onAbilityActivate
                    );
                    world.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2);

                    next.run();
                },
                next -> new AbilityRunnable() {
                    StorageMinecart dropPod;
                    boolean decelerated = false;

                    @Override
                    protected void start() {
                        abilityActive.set(true);

                        dropPod = world.spawn(location.clone().add(0, 300, 0), StorageMinecart.class, false, entity -> {
                            entity.setGlowing(true);
                            entity.setDerailedVelocityMod(new Vector());
                            entity.setMaxSpeed(0);
                            entity.setInvulnerable(true);
                            entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.getCodeName(), ownerName)));
                        });
                        Random random = new Random();
                        LootContext context = new LootContext.Builder(dropPod.getLocation())
                                .killer(player)
                                .lootedEntity(dropPod)
                                .build();
                        LootTable lootTable = new LifelinePackageLootTable();
                        lootTable.populateLoot(random, context);
                        lootTable.fillInventory(dropPod.getInventory(), random, context);
                    }

                    @Override
                    protected void tick() {
                        if (!dropPod.isValid() || dropPod.isOnGround() || dropPod.isInWater()) {
                            cancel();
                        }
                        RayTraceResult result = dropPod.getWorld().rayTraceBlocks(dropPod.getLocation(), new Vector(0, -1, 0), 20, FluidCollisionMode.ALWAYS, true);
                        if (result != null && result.getHitBlock() != null) {
                            if (!decelerated) {
                                for (int i = 0; i < 100; i++) {
                                    double angle = Math.random() * Math.PI * 2;
                                    double magnitude = Math.random() * 0.1 + 0.45;
                                    double x = Math.cos(angle);
                                    double z = Math.sin(angle);
                                    dropPod.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, dropPod.getLocation(), 0, x, 0, z, magnitude, null, true);
                                }
                                decelerated = true;
                            }
                            if (dropPod.getVelocity().getY() < -0.03)
                                dropPod.setVelocity(dropPod.getVelocity().add(new Vector(0, 0.037, 0)));
                            dropPod.getWorld().playSound(dropPod.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.1f, 0.1f);
                            dropPod.getWorld().spawnParticle(Particle.SMOKE_NORMAL, dropPod.getLocation().add(0.49, 0.5, 0.49), 0, Math.random() * 0.1 - 0.05, -1, Math.random() * 0.1 - 0.05, 1);
                            dropPod.getWorld().spawnParticle(Particle.SMOKE_NORMAL, dropPod.getLocation().add(-0.49, 0.5, 0.49), 0, Math.random() * 0.1 - 0.05, -1, Math.random() * 0.1 - 0.05, 1);
                            dropPod.getWorld().spawnParticle(Particle.SMOKE_NORMAL, dropPod.getLocation().add(0.49, 0.5, -0.49), 0, Math.random() * 0.1 - 0.05, -1, Math.random() * 0.1 - 0.05, 1);
                            dropPod.getWorld().spawnParticle(Particle.SMOKE_NORMAL, dropPod.getLocation().add(-0.49, 0.5, -0.49), 0, Math.random() * 0.1 - 0.05, -1, Math.random() * 0.1 - 0.05, 1);
                        } else {
                            if (dropPod.getLocation().getY() >= 190 && Math.random() < 0.2)
                                dropPod.getWorld().spawnParticle(Particle.FLASH, dropPod.getLocation(), 1, 0, 0, 0, 0, null, true);
                            dropPod.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, dropPod.getLocation(), 1, 0.1, 0.1, 0.1, 0, null, true);
                        }
                    }

                    @Override
                    protected void end() {
                        abilityActive.set(false);
                        dropPod.setInvulnerable(false);
                        dropPod.getWorld().playSound(dropPod.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_HURT, 1, 0.7f);
                        dropPod.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, dropPod.getLocation(), 20, 0.5, 0.1, 0.5, 0.02);
                    }
                }.runTaskTimer(this, 0, 1)
        ).execute();
    }
}
