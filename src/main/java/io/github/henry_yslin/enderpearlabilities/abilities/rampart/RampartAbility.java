package io.github.henry_yslin.enderpearlabilities.abilities.rampart;

import io.github.henry_yslin.enderpearlabilities.abilities.*;
import io.github.henry_yslin.enderpearlabilities.managers.interactionlock.InteractionLockManager;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.concurrent.atomic.AtomicBoolean;

public class RampartAbility extends Ability {

    static final double PROJECTILE_SPEED = 4;

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        super.setConfigDefaults(config);
        config.addDefault("charge-up", 20);
        config.addDefault("duration", 200);
        config.addDefault("cooldown", 60);
    }

    public RampartAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("rampart")
                .name("Mobile Minigun \"Sheila\"")
                .origin("Apex - Rampart")
                .description("Wield a mobile minigun with a single high capacity magazine. Cooldown is increased by the number of shots fired.")
                .usage("Right click to bring up Sheila. Sneak to spin up and fire, switch away from holding ender pearls to holster the minigun. Right click with the ender pearl again to start cooldown.")
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

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    final AtomicBoolean isSneaking = new AtomicBoolean(false);
    AbilityRunnable minigun;

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
        if (minigun != null)
            if (!minigun.isCancelled())
                minigun.cancel();
    }

    @EventHandler
    public synchronized void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        isSneaking.set(event.isSneaking());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;
        if (chargingUp.get()) return;
        if (abilityActive.get() && minigun != null && !minigun.isCancelled()) {
            minigun.cancel();
            return;
        }

        PlayerUtils.consumeEnderPearl(player);

        new FunctionChain(
                next -> AbilityUtils.chargeUpSequence(this, player, 20, chargingUp, next),
                next -> (minigun = new AbilityRunnable() {
                    BossBar spinUpBossBar;
                    BossBar magazineBossBar;
                    int magazine;
                    int spinUpTicks;

                    @Override
                    protected void start() {
                        abilityActive.set(true);
                        magazine = info.duration;
                        spinUpTicks = info.chargeUp;
                        spinUpBossBar = Bukkit.createBossBar("Spinning up", BarColor.WHITE, BarStyle.SOLID);
                        magazineBossBar = Bukkit.createBossBar(info.name, BarColor.PURPLE, BarStyle.SOLID);
                        magazineBossBar.addPlayer(player);
                    }

                    private void displayMagazineBar() {
                        if (magazineBossBar.getPlayers().size() == 0) {
                            magazineBossBar.addPlayer(player);
                            spinUpBossBar.removeAll();
                        }
                        magazineBossBar.setProgress(magazine / (double) info.duration);
                    }

                    private void displaySpinUpBar() {
                        if (spinUpBossBar.getPlayers().size() == 0) {
                            spinUpBossBar.addPlayer(player);
                            magazineBossBar.removeAll();
                        }
                        spinUpBossBar.setProgress(1 - spinUpTicks / (double) info.chargeUp);
                    }

                    private Vector randomizeVelocity(Vector vector) {
                        Vector horizontal = vector.getCrossProduct(new Vector(0, 1, 0)).normalize();
                        Vector vertical = vector.getCrossProduct(horizontal).normalize();
                        return vector.add(horizontal.multiply(Math.random() * 0.2 - 0.1)).add(vertical.multiply(Math.random() * 0.2 - 0.1));
                    }

                    @Override
                    protected void tick() {
                        if (magazine <= 0 || !player.isValid()) {
                            cancel();
                            return;
                        }
                        if (player.getInventory().getItemInMainHand().getType() == Material.ENDER_PEARL) {
                            player.addPotionEffect(PotionEffectType.SLOW.createEffect(5, 1));
                        }
                        boolean firing = isSneaking.get();
                        if (firing)
                            InteractionLockManager.getInstance().lockInteraction(player);
                        else
                            InteractionLockManager.getInstance().unlockInteraction(player);
                        if (!firing || player.getInventory().getItemInMainHand().getType() != Material.ENDER_PEARL) {
                            if (spinUpTicks < info.chargeUp) {
                                spinUpTicks += 2;
                                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, (info.chargeUp - spinUpTicks) / (float) info.chargeUp * 2);
                            }
                            displayMagazineBar();
                            return;
                        }
                        if (spinUpTicks > 0) {
                            spinUpTicks--;
                            player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, player.getLocation(), 2, 0.2, 0.2, 0.2, 0.05);
                            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, (info.chargeUp - spinUpTicks) / (float) info.chargeUp * 2);
                            displaySpinUpBar();
                        } else {
                            magazine--;
                            displayMagazineBar();
                            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 2);
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.3f, 0);
                            Arrow arrow = player.launchProjectile(Arrow.class, randomizeVelocity(player.getLocation().getDirection().multiply(PROJECTILE_SPEED)));
                            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                            arrow.setTicksLived(1180);
                            arrow.setPierceLevel(2);
                            arrow.setDamage(3);
                            arrow.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.codeName, ownerName)));
                        }
                    }

                    @Override
                    protected void end() {
                        magazineBossBar.removeAll();
                        spinUpBossBar.removeAll();
                        abilityActive.set(false);
                        InteractionLockManager.getInstance().unlockInteraction(player);
                        if (magazine == info.duration)
                            cooldown.startCooldown(20);
                        else
                            cooldown.startCooldown(info.cooldown + (info.duration - magazine) * 8);
                    }
                }).runTaskTimer(this, 0, 1)
        ).execute();
    }
}
