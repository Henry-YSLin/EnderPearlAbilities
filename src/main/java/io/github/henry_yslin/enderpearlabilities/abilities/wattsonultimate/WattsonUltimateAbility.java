package io.github.henry_yslin.enderpearlabilities.abilities.wattsonultimate;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityCouple;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.ItemStackUtils;
import io.github.henry_yslin.enderpearlabilities.utils.WorldUtils;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class WattsonUltimateAbility extends Ability<WattsonUltimateAbilityInfo> {

    static final int INTERCEPTION_PER_TICK = Integer.MAX_VALUE;

    public WattsonUltimateAbility(Plugin plugin, WattsonUltimateAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    AtomicReference<EnderCrystal> pylon = new AtomicReference<>();

    @Override
    public boolean isActive() {
        return abilityActive.get();
    }

    @Override
    public boolean isChargingUp() {
        return chargingUp.get();
    }

    private boolean shouldIntercept(Entity entity) {
        if (!entity.isValid()) return false;
        if (entity instanceof Projectile projectile) {
            if (Objects.equals(projectile.getShooter(), player)) return false;
        }

        EntityType[] blacklist = {
                EntityType.SPLASH_POTION,
                EntityType.ARROW,
                EntityType.SPECTRAL_ARROW,
                EntityType.FIREBALL,
                EntityType.SMALL_FIREBALL,
                EntityType.FIREWORK,
                EntityType.DRAGON_FIREBALL,
                EntityType.PRIMED_TNT
        };

        if (entity instanceof Creeper creeper) {
            return creeper.getMaxFuseTicks() - creeper.getFuseTicks() < 10;
        } else return Arrays.stream(blacklist).anyMatch(t -> entity.getType() == t);
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
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (!AbilityUtils.verifyAbilityCouple(this, event.getEntity())) return;
        event.setCancelled(true);
        event.getEntity().remove();
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!AbilityUtils.verifyAbilityCouple(this, event.getEntity())) return;
        Player damager = null;
        if (event.getDamager() instanceof Player player) {
            damager = player;
        } else if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player player)
                damager = player;
        }
        if (damager != null && damager.getName().equals(ownerName)) return;
        if (chargingUp.get()) return;
        event.getEntity().getWorld().createExplosion(event.getEntity().getLocation(), 3, false, false);
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;

        event.setCancelled(true);

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        if (cooldown.isCoolingDown()) return;

        if (abilityActive.get()) return;

        World world = player.getWorld();

        if (!event.getClickedBlock().getType().isSolid()) return;
        Location location = event.getClickedBlock().getLocation();
        location.add(0.5, 2, 0.5);
        if (location.getBlock().getType() != Material.AIR) return;

        abilityActive.set(true);
        AbilityUtils.consumeEnderPearl(this, player);
        EnderPearlAbilities.getInstance().emitEvent(
                EventListener.class,
                new AbilityActivateEvent(this),
                EventListener::onAbilityActivate
        );

        new FunctionChain(
                next -> {
                    if (pylon.get() != null && pylon.get().isValid()) {
                        pylon.get().remove();
                    }
                    EnderCrystal crystal = world.spawn(location, EnderCrystal.class, false, entity -> {
                        entity.setGravity(false);
                        entity.setGlowing(true);
                        entity.setShowingBottom(true);
                        entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.getCodeName(), ownerName)));
                    });
                    pylon.set(crystal);

                    WorldUtils.spawnParticleCubeOutline(location.clone().add(-8, -8, -8), location.clone().add(8, 8, 8), Particle.END_ROD, 5, true);
                    world.playSound(crystal.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 0.5f);

                    next.run();
                },
                next -> new AbilityRunnable() {
                    @Override
                    protected void start() {
                        chargingUp.set(true);
                    }

                    @Override
                    protected void tick() {
                        world.spawnParticle(Particle.WHITE_ASH, pylon.get().getLocation(), 4, 0.5, 0.5, 0.5, 0.02);
                    }

                    @Override
                    protected void end() {
                        chargingUp.set(false);
                        if (this.hasCompleted())
                            next.run();
                        else
                            pylon.get().remove();
                    }
                }.runTaskRepeated(this, 0, 2, info.getChargeUp() / 2),
                next -> new AbilityRunnable() {
                    BossBar bossbar;
                    int beamTick;

                    @Override
                    protected synchronized void start() {
                        bossbar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.getName(), BarColor.PURPLE, BarStyle.SOLID);
                        bossbar.addPlayer(player);
                    }

                    @Override
                    protected synchronized void tick() {
                        EnderCrystal crystal = pylon.get();
                        if (!abilityActive.get() || !crystal.isValid()) {
                            cancel();
                            return;
                        }
                        bossbar.setProgress(count / (double) info.getDuration());
                        if (count % 40 == 0)
                            world.playSound(crystal.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1, 0.5f);
                        if (count % 5 == 0)
                            world.spawnParticle(Particle.END_ROD, crystal.getLocation(), 2, 0.5, 0.5, 0.5, 0.02);
                        if (beamTick <= 0) crystal.setBeamTarget(null);
                        else beamTick--;
                        int interceptions = 0;
                        for (Entity entity : world.getNearbyEntities(crystal.getLocation(), 8, 8, 8)) {
                            if (shouldIntercept(entity) && interceptions < INTERCEPTION_PER_TICK) {
                                crystal.setBeamTarget(entity.getLocation().add(0, -2, 0));
                                beamTick = 5;
                                world.spawnParticle(Particle.SMOKE_LARGE, entity.getLocation(), 5, 0.1, 0.1, 0.1, 0.01);
                                world.playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.1f, 0);
                                entity.remove();
                                interceptions++;
                            } else if (count % 5 == 1) {
                                if (entity instanceof Player player) {
                                    ItemStackUtils.damageTool(player.getInventory().getHelmet(), -1);
                                    ItemStackUtils.damageTool(player.getInventory().getChestplate(), -1);
                                    ItemStackUtils.damageTool(player.getInventory().getLeggings(), -1);
                                    ItemStackUtils.damageTool(player.getInventory().getBoots(), -1);
                                }
                            }
                        }
                    }

                    @Override
                    protected synchronized void end() {
                        bossbar.removeAll();
                        if (pylon.get().isValid())
                            pylon.get().remove();
                        abilityActive.set(false);
                        cooldown.setCooldown(info.getCooldown());
                        next.run();
                    }
                }.runTaskRepeated(this, 0, 1, info.getDuration())
        ).execute();
    }
}
