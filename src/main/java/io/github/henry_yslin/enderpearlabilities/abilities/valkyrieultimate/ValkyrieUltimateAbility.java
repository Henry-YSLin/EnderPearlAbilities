package io.github.henry_yslin.enderpearlabilities.abilities.valkyrieultimate;

import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henry_yslin.enderpearlabilities.managers.interactionlock.InteractionLockManager;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.MathUtils;
import io.github.henry_yslin.enderpearlabilities.utils.PlayerUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ValkyrieUltimateAbility extends Ability<ValkyrieUltimateAbilityInfo> {

    static final int MAX_LAUNCH_HEIGHT = 64;
    static final int MIN_LAUNCH_HEIGHT = 30;
    static final int INITIAL_BURN_DURATION = 30;
    static final int SCAN_INTERVAL = 20;
    static final double MIN_SCAN_RANGE = 30;
    static final double MAX_SCAN_RANGE = 250;

    public ValkyrieUltimateAbility(Plugin plugin, ValkyrieUltimateAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicInteger chargeUpDuration = new AtomicInteger(0);
    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    AbilityRunnable flightRunnable;

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
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        if (flightRunnable != null && !flightRunnable.isCancelled())
            flightRunnable.cancel();
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation(), true)) return;

        event.setCancelled(true);

        if (cooldown.isCoolingDown()) return;
        if (abilityActive.get()) return;

        if (chargingUp.get()) {
            if (chargeUpDuration.get() >= info.getChargeUp()) {
                chargingUp.set(false);
                abilityActive.set(true);
            }
        } else if (!InteractionLockManager.getInstance().isInteractionLocked(player)) {
            Ability<?> ability = this;
            new FunctionChain(
                    next -> new AbilityRunnable() {
                        BossBar bossbar;
                        Location groundLocation;
                        Location stableLocation;

                        @Override
                        protected void start() {
                            bossbar = Bukkit.createBossBar("Charging up", BarColor.WHITE, BarStyle.SOLID);
                            bossbar.addPlayer(player);
                            InteractionLockManager.getInstance().lockInteraction(player);
                            chargingUp.set(true);
                            chargeUpDuration.set(0);
                            groundLocation = player.getLocation();
                            stableLocation = null;
                        }

                        @Override
                        protected void tick() {
                            if (!chargingUp.get()) {
                                cancel();
                                return;
                            }
                            bossbar.setProgress(Math.min(1, chargeUpDuration.incrementAndGet() / (double) info.getChargeUp()));
                            boolean mainHandPearl = player.getInventory().getItemInMainHand().getType() == Material.ENDER_PEARL;
                            boolean offHandPearl = player.getInventory().getItemInOffHand().getType() == Material.ENDER_PEARL;
                            boolean shouldContinue = ability.getInfo().getActivation() == ActivationHand.MainHand && mainHandPearl ||
                                    ability.getInfo().getActivation() == ActivationHand.OffHand && offHandPearl;
                            if (!player.isValid()) shouldContinue = false;
                            if (!player.isOnline()) shouldContinue = false;
                            if (!player.getWorld().equals(groundLocation.getWorld())) shouldContinue = false;
                            if (groundLocation.toVector().setY(0).distance(player.getLocation().toVector().setY(0)) > 1)
                                shouldContinue = false;
                            if (shouldContinue)
                                if (stableLocation != null && stableLocation.distance(player.getLocation()) > 1)
                                    shouldContinue = false;
                            if (shouldContinue) {
                                RayTraceResult result = player.getWorld().rayTraceBlocks(player.getEyeLocation(), new Vector(0, 1, 0), MIN_LAUNCH_HEIGHT, FluidCollisionMode.NEVER, true);
                                if (result != null) {
                                    player.sendTitle(" ", ChatColor.RED + "Need vertical clearance", 5, 20, 20);
                                    shouldContinue = false;
                                }
                            }
                            if (shouldContinue) {
                                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.05f, 0.3f);
                                double y = player.getVelocity().getY();
                                player.setVelocity(player.getVelocity().multiply(0.8).setY(y));
                                if (player.getLocation().getY() < groundLocation.getY()) {
                                    groundLocation.setY(player.getLocation().getY());
                                }
                                player.setGravity(false);
                                if (player.getLocation().getY() - groundLocation.getY() < 1) {
                                    player.setVelocity(player.getVelocity().add(new Vector(0, 0.1, 0)));
                                } else {
                                    player.setVelocity(player.getVelocity().setY(0));
                                    if (stableLocation == null) {
                                        stableLocation = player.getLocation();
                                        player.teleport(stableLocation);
                                    }
                                }
                                player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 2, 0.1, 0.1, 0.1, 0.05);
                            } else {
                                chargingUp.set(false);
                                abilityActive.set(false);
                                cooldown.setCooldown(100);
                                cancel();
                            }
                        }

                        @Override
                        protected void end() {
                            bossbar.removeAll();
                            player.setGravity(true);
                            if (abilityActive.get()) {
                                next.run();
                            } else {
                                InteractionLockManager.getInstance().unlockInteraction(player);
                            }
                        }
                    }.runTaskTimer(this, 0, 1),
                    next -> new AbilityRunnable() {
                        Location groundLocation;
                        Location lastLocation;
                        boolean allowGlide = false;
                        int stationaryTick = 0;

                        @Override
                        protected void start() {
                            PlayerUtils.consumeEnderPearl(player);
                            groundLocation = player.getLocation();
                        }

                        @Override
                        protected void tick() {
                            if (!player.isValid()) {
                                cancel();
                                return;
                            }
                            double propulsion;
                            if (info.getDuration() - count == INITIAL_BURN_DURATION) {
                                player.setVelocity(player.getVelocity().add(new Vector(0, 0.2, 0)));
                                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0);
                            }
                            if (info.getDuration() - count >= INITIAL_BURN_DURATION)
                                propulsion = 0.2;
                            else {
                                propulsion = Math.min((info.getDuration() - count) * 0.0005 + 0.075, 0.09);
                                player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 2, 0.1, 0.1, 0.1, 0.05);
                            }
                            boolean shouldPropel = player.getLocation().getY() - groundLocation.getY() < MAX_LAUNCH_HEIGHT;
                            if (shouldPropel) {
                                if (lastLocation != null && MathUtils.almostEqual(player.getLocation().getY(), lastLocation.getY())) {
                                    stationaryTick++;
                                    if (stationaryTick > 20) {
                                        shouldPropel = false;
                                    }
                                } else {
                                    stationaryTick = 0;
                                }
                            }
                            if (shouldPropel) {
                                RayTraceResult result = player.getWorld().rayTraceBlocks(player.getEyeLocation(), new Vector(0, 1, 0), 5, FluidCollisionMode.NEVER, true);
                                if (result != null) shouldPropel = false;
                            }
                            if (shouldPropel) {
                                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.2f, 2);
                                player.setVelocity(player.getVelocity().add(new Vector(0, propulsion, 0)));
                                player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 2, 0.1, 0.1, 0.1, 0.05);
                                lastLocation = player.getLocation();
                            } else {
                                allowGlide = true;
                                cancel();
                            }
                        }

                        @Override
                        protected void end() {
                            InteractionLockManager.getInstance().unlockInteraction(player);
                            if (allowGlide)
                                next.run();
                            else
                                abilityActive.set(false);
                        }
                    }.runTaskRepeated(this, 0, 1, info.getDuration()),
                    next -> {
                        if (flightRunnable != null && !flightRunnable.isCancelled())
                            flightRunnable.cancel();
                        (flightRunnable = new AbilityRunnable() {
                            ItemStack chestplate;
                            int scanInterval;

                            @Override
                            protected void start() {
                                chestplate = player.getInventory().getChestplate();
                                ItemStack elytra = new ItemStack(Material.ELYTRA, 1);
                                ItemMeta meta = Bukkit.getServer().getItemFactory().getItemMeta(Material.ELYTRA);
                                if (meta != null)
                                    meta.setUnbreakable(true);
                                elytra.setItemMeta(meta);
                                elytra.addEnchantment(Enchantment.BINDING_CURSE, 1);
                                elytra.addEnchantment(Enchantment.VANISHING_CURSE, 1);
                                player.getInventory().setChestplate(elytra);
                                player.setGliding(true);
                                scanInterval = SCAN_INTERVAL;
                            }

                            @Override
                            protected void tick() {
                                //noinspection deprecation
                                if (!player.isValid() || player.isOnGround()) {
                                    cancel();
                                    return;
                                }
                                player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation(), 2, 0.1, 0.1, 0.1, 0, null, true);

                                double groundHeight = 256;
                                RayTraceResult result = player.getWorld().rayTraceBlocks(player.getLocation(), new Vector(0, -1, 0), 256, FluidCollisionMode.ALWAYS, true);
                                if (result != null) {
                                    double groundY = result.getHitPosition().getY();
                                    groundHeight = player.getLocation().getY() - groundY;
                                    if (groundHeight < 1)
                                        cancel();
                                }

                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GREEN + String.format("GND: %dm  Y: %dm  SPD: %dm/s", (int) groundHeight, (int) player.getLocation().getY(), (int) (player.getVelocity().length() * 20))));

                                scanInterval--;
                                if (scanInterval <= 0) {
                                    scanInterval = SCAN_INTERVAL;
                                    player.getWorld().getNearbyEntities(
                                            player.getEyeLocation(),
                                            MAX_SCAN_RANGE, MAX_SCAN_RANGE, MAX_SCAN_RANGE,
                                            entity -> entity instanceof LivingEntity && entity.getLocation().distanceSquared(player.getLocation()) > MIN_SCAN_RANGE * MIN_SCAN_RANGE
                                    ).forEach(entity -> player.spawnParticle(Particle.FLASH, entity.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0, null));
                                }
                            }

                            @Override
                            protected void end() {
                                player.getInventory().setChestplate(chestplate);
                                abilityActive.set(false);
                                cooldown.setCooldown(info.getCooldown());
                            }
                        }).runTaskTimer(this, 0, 1);
                    }
            ).execute();
        }
    }
}
