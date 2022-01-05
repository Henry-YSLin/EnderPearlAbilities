package io.github.henry_yslin.enderpearlabilities.abilities.revenantultimate;

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
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class RevenantUltimateAbility extends Ability<RevenantUltimateAbilityInfo> {

    public RevenantUltimateAbility(Plugin plugin, RevenantUltimateAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    AtomicReference<Blaze> totem = new AtomicReference<>();

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
    public void onEntityDeath(EntityDeathEvent event) {
        if (!AbilityUtils.verifyAbilityCouple(this, event.getEntity())) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.getName().equals(ownerName)) return;
        if (!abilityActive.get()) return;
        Blaze blaze = totem.get();
        if (blaze == null || !blaze.isValid()) return;
        if (player.getHealth() - event.getFinalDamage() <= 0) {
            event.setCancelled(true);
            player.setHealth(EntityUtils.getMaxHealth(player) / 2);
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            Bukkit.broadcastMessage(player.getName() + " was protected from death");

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1, 2);
            player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getEyeLocation(), 20, 0.5, 0.5, 0.5, 0);
            blaze.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, blaze.getEyeLocation(), 20, 0.5, 0.5, 0.5, 0);
            if (player.getWorld() == blaze.getWorld())
                WorldUtils.spawnParticleLine(player.getEyeLocation(), blaze.getEyeLocation(), Particle.SOUL_FIRE_FLAME, 1, true);

            player.teleport(blaze.getLocation());
        }
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
        location.add(0.5, 1, 0.5);
        if (location.getBlock().getType() != Material.AIR) return;

        AbilityUtils.consumeEnderPearl(this, player);
        EnderPearlAbilities.getInstance().emitEvent(
                EventListener.class,
                new AbilityActivateEvent(this),
                EventListener::onAbilityActivate
        );

        new FunctionChain(
                next -> {
                    if (totem.get() != null && totem.get().isValid()) {
                        totem.get().remove();
                    }
                    Blaze blaze = world.spawn(location, Blaze.class, false, entity -> {
                        entity.setGravity(false);
                        entity.setGlowing(true);
                        entity.setAI(false);
                        entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.getCodeName(), ownerName)));
                    });
                    totem.set(blaze);

                    totem.get().getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 0);

                    next.run();
                },
                next -> new AbilityRunnable() {
                    BossBar bossBar;

                    @Override
                    protected void start() {
                        bossBar = Bukkit.createBossBar("Charging up", BarColor.WHITE, BarStyle.SOLID);
                        bossBar.addPlayer(player);
                        chargingUp.set(true);
                    }

                    @Override
                    protected void tick() {
                        if (totem.get() == null || !totem.get().isValid() || !player.isValid()) {
                            cancel();
                            return;
                        }
                        bossBar.setProgress(1 - count / (float) info.getChargeUp() * 2);
                        world.spawnParticle(Particle.FIREWORKS_SPARK, totem.get().getEyeLocation(), 4, 0.1, 0.1, 0.1, 0.02);
                    }

                    @Override
                    protected void end() {
                        bossBar.removeAll();
                        chargingUp.set(false);
                        if (this.hasCompleted())
                            next.run();
                        else {
                            player.sendMessage(ChatColor.RED + info.getName() + " was destroyed");
                            totem.get().remove();
                            cooldown.setCooldown(5 * 20);
                        }
                    }
                }.runTaskRepeated(this, 0, 2, info.getChargeUp() / 2),
                next -> new AbilityRunnable() {
                    BossBar bossBar;

                    @Override
                    protected void start() {
                        bossBar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.getName(), BarColor.PURPLE, BarStyle.SOLID);
                        bossBar.addPlayer(player);
                        abilityActive.set(true);
                        totem.get().getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1, 0);
                        totem.get().getWorld().spawnParticle(Particle.SOUL, totem.get().getEyeLocation(), 20, 0.5, 0.5, 0.5, 0.02);
                    }

                    @Override
                    protected void tick() {
                        if (totem.get() == null || !totem.get().isValid()) {
                            cancel();
                            return;
                        }
                        bossBar.setProgress(count / (float) info.getDuration() * 5);
                        totem.get().getWorld().spawnParticle(Particle.SOUL, totem.get().getEyeLocation(), 1, 0.1, 0.1, 0.1, 0.02);
                    }

                    @Override
                    protected void end() {
                        bossBar.removeAll();
                        if (!hasCompleted())
                            player.sendMessage(ChatColor.RED + info.getName() + " was destroyed");
                        abilityActive.set(false);
                        totem.get().remove();
                        totem.set(null);
                        cooldown.setCooldown(info.getCooldown());
                    }
                }.runTaskRepeated(this, 0, 5, info.getDuration() / 5)
        ).execute();
    }
}
