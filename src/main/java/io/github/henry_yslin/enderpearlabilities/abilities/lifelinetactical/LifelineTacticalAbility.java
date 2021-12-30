package io.github.henry_yslin.enderpearlabilities.abilities.lifelinetactical;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityCouple;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.WorldUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class LifelineTacticalAbility extends Ability<LifelineTacticalAbilityInfo> {

    static final double HEAL_RANGE = 3;

    public LifelineTacticalAbility(Plugin plugin, LifelineTacticalAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);

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

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!AbilityUtils.verifyAbilityCouple(this, event.getEntity())) return;
        event.getDrops().clear();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;

        event.setCancelled(true);

        if (abilityActive.get()) return;
        if (chargingUp.get()) return;
        if (cooldown.isCoolingDown()) return;

        AtomicReference<LivingEntity> drone = new AtomicReference<>();

        new FunctionChain(
                next -> {
                    AbilityUtils.consumeEnderPearl(this, player);
                    EnderPearlAbilities.getInstance().emitEvent(
                            EventListener.class,
                            new AbilityActivateEvent(this),
                            EventListener::onAbilityActivate
                    );
                    drone.set(player.getWorld().spawn(player.getLocation().add(0, 1, 0), Axolotl.class, false, entity -> {
                        entity.setAgeLock(true);
                        entity.setAge(-1);
                        entity.setVariant(Axolotl.Variant.BLUE);
                        entity.setAware(false);
                        entity.setGravity(false);
                        entity.setCollidable(false);
                        entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.getCodeName(), ownerName)));
                        entity.setVelocity(player.getLocation().getDirection().setY(0).normalize().multiply(0.4));
                    }));
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                    player.addPotionEffect(PotionEffectType.SLOW.createEffect(info.getChargeUp() / 2, 1));
                    next.run();
                },
                next -> new AbilityRunnable() {
                    LivingEntity axolotl;

                    @Override
                    protected void start() {
                        axolotl = drone.get();
                    }

                    @Override
                    protected void tick() {
                        if (!axolotl.isValid()) {
                            cancel();
                            return;
                        }
                        axolotl.setVelocity(axolotl.getVelocity().multiply(0.8));
                        axolotl.getWorld().spawnParticle(Particle.SMOKE_NORMAL, axolotl.getEyeLocation(), 2, 0.1, 0.1, 0.1, 0.01);
                    }

                    @Override
                    protected void end() {
                        if (hasCompleted()) {
                            next.run();
                        } else {
                            axolotl.remove();
                        }
                    }
                }.runTaskRepeated(this, 0, 1, info.getChargeUp()),
                next -> new AbilityRunnable() {
                    LivingEntity axolotl;
                    BossBar bossbar;

                    @Override
                    protected synchronized void start() {
                        abilityActive.set(true);
                        axolotl = drone.get();
                        bossbar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.getName(), BarColor.PURPLE, BarStyle.SOLID);
                        bossbar.addPlayer(player);
                    }

                    @Override
                    protected synchronized void tick() {
                        if (!abilityActive.get() || !axolotl.isValid()) {
                            cancel();
                        }
                        bossbar.setProgress(count / (double) info.getDuration() * 5);
                        axolotl.setVelocity(axolotl.getVelocity().multiply(0.8));
                        axolotl.getWorld().spawnParticle(Particle.END_ROD, axolotl.getEyeLocation(), 2, 0.1, 0.1, 0.1, 0.01);
                        for (Entity entity : axolotl.getNearbyEntities(HEAL_RANGE, HEAL_RANGE, HEAL_RANGE)) {
                            if (entity instanceof Player receiver) {
                                receiver.addPotionEffect(PotionEffectType.REGENERATION.createEffect(8, 3));
                                WorldUtils.spawnParticleLine(axolotl.getLocation(), receiver.getLocation().add(0, 1, 0), Particle.ELECTRIC_SPARK, 2, false);
                            }
                        }
                    }

                    @Override
                    protected synchronized void end() {
                        bossbar.removeAll();
                        axolotl.remove();
                        abilityActive.set(false);
                        cooldown.setCooldown(info.getCooldown());
                        next.run();
                    }
                }.runTaskRepeated(this, 0, 5, info.getDuration() / 5)
        ).execute();
    }
}
