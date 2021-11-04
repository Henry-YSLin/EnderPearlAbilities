package io.github.henry_yslin.enderpearlabilities.abilities.valkyrieultimate;

import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henry_yslin.enderpearlabilities.managers.interactionlock.InteractionLockManager;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.PlayerUtils;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ValkyrieUltimateAbility extends Ability {

    static final int MAX_LAUNCH_HEIGHT = 64;
    static final int MIN_LAUNCH_HEIGHT = 10;
    static final int INITIAL_BURN_DURATION = 30;

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 40);
        config.addDefault("duration", 200);
        config.addDefault("cooldown", 1200);
    }

    public ValkyrieUltimateAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("valkyrie-ultimate")
                .name("Skyward Dive")
                .origin("Apex - Valkyrie")
                .description("Press once to prepare for launch. Press again to launch into the air and skydive.")
                .usage("Right click to prepare for launch. Right click again to launch. Switch away from ender pearl to cancel.")
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

    final AtomicInteger chargeUpDuration = new AtomicInteger(0);
    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);

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
        cooldown.startCooldown(info.cooldown);
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation, true)) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;
        if (abilityActive.get()) return;

        if (chargingUp.get()) {
            if (chargeUpDuration.get() >= info.chargeUp) {
                chargingUp.set(false);
                abilityActive.set(true);
            }
        } else if (!InteractionLockManager.getInstance().isInteractionLocked(player)) {
            new FunctionChain(
                    next -> new AbilityRunnable() {
                        BossBar bossbar;
                        Location groundLocation;

                        @Override
                        protected void start() {
                            bossbar = Bukkit.createBossBar("Charging up", BarColor.WHITE, BarStyle.SOLID);
                            bossbar.addPlayer(player);
                            InteractionLockManager.getInstance().lockInteraction(player);
                            chargingUp.set(true);
                            chargeUpDuration.set(0);
                            groundLocation = player.getLocation();
                        }

                        @Override
                        protected void tick() {
                            if (!chargingUp.get()) {
                                cancel();
                                return;
                            }
                            bossbar.setProgress(Math.min(1, chargeUpDuration.incrementAndGet() / (double) info.chargeUp));
                            boolean mainHandPearl = player.getInventory().getItemInMainHand().getType() == Material.ENDER_PEARL;
                            boolean offHandPearl = player.getInventory().getItemInOffHand().getType() == Material.ENDER_PEARL;
                            boolean shouldContinue = executor.getInfo().activation == ActivationHand.MainHand && mainHandPearl ||
                                    executor.getInfo().activation == ActivationHand.OffHand && offHandPearl;
                            if (!player.isValid()) shouldContinue = false;
                            if (!player.getWorld().equals(groundLocation.getWorld())) shouldContinue = false;
                            if (groundLocation.toVector().setY(0).distance(player.getLocation().toVector().setY(0)) > 1)
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
                                if (player.getLocation().getY() - groundLocation.getY() < 1.5) {
                                    player.setVelocity(player.getVelocity().add(new Vector(0, 0.1, 0)));
                                } else {
                                    player.setVelocity(player.getVelocity().setY(0));
                                }
                                player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 2, 0.1, 0.1, 0.1, 0.05);
                            } else {
                                chargingUp.set(false);
                                abilityActive.set(false);
                                cooldown.startCooldown(100);
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
                            if (info.duration - count == INITIAL_BURN_DURATION) {
                                player.setVelocity(player.getVelocity().add(new Vector(0, 1.1, 0)));
                                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0);
                            }
                            if (info.duration - count >= INITIAL_BURN_DURATION)
                                propulsion = 1.1;
                            else {
                                propulsion = Math.min((info.duration - count) * 0.0005 + 0.075, 0.09);
                                player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 2, 0.1, 0.1, 0.1, 0.05);
                            }
                            boolean shouldPropel = player.getLocation().getY() - groundLocation.getY() < MAX_LAUNCH_HEIGHT;
                            if (shouldPropel) {
                                if (player.getLocation().equals(lastLocation)) shouldPropel = false;
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
                    }.runTaskRepeated(this, 0, 1, info.duration),
                    next -> new AbilityRunnable() {
                        ItemStack chestplate;

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
                        }

                        @Override
                        protected void tick() {
                            //noinspection deprecation
                            if (!player.isValid() || player.isOnGround()) {
                                cancel();
                                return;
                            }
                            player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation(), 2, 0.1, 0.1, 0.1, 0, null, true);
                            RayTraceResult result = player.getWorld().rayTraceBlocks(player.getLocation(), new Vector(0, -1, 0), 1, FluidCollisionMode.ALWAYS, true);
                            if (result != null) {
                                cancel();
                            }
                        }

                        @Override
                        protected void end() {
                            player.getInventory().setChestplate(chestplate);
                            abilityActive.set(false);
                            cooldown.startCooldown(info.cooldown);
                        }
                    }.runTaskTimer(this, 0, 1)
            ).execute();
        }
    }
}
