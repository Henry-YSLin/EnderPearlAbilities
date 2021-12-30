package io.github.henry_yslin.enderpearlabilities.abilities.titans;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityCooldown;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityCouple;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.WorldUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public abstract class TitanAbility<TInfo extends TitanInfo> extends Ability<TInfo> {

    static final int ACTION_COOLDOWN = 5;
    static final int ENERGY_SIPHON_RANGE = 64;

    public TitanAbility(Plugin plugin, TInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    final AtomicReference<Action> pendingAction = new AtomicReference<>();
    final AtomicInteger actionCooldown = new AtomicInteger(0);
    final AtomicReference<IronGolem> titan = new AtomicReference<>();
    AbilityRunnable titanControlRunnable;

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
            chargingUp.set(false);
            abilityActive.set(false);
            cooldown.setCooldown(info.getCooldown());
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (player != null) {
            chargingUp.set(false);
            abilityActive.set(false);
            cooldown.setCooldown(info.getCooldown());
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!player.getName().equals(ownerName)) return;
        if (event.getEntity().equals(titan.get())) return;
        if (!cooldown.isCoolingDown()) return;
        cooldown.setCooldown(cooldown.getCooldownTicks() - (int) (event.getFinalDamage() * 20));
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation(), (int) event.getFinalDamage(), 0.5, 0.5, 0.5, 0.1);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!player.getName().equals(ownerName)) return;
        if (!AbilityUtils.verifyAbilityCouple(this, event.getRightClicked())) return;
        event.setCancelled(true);
        if (event.getRightClicked().getPassengers().size() > 0) return;
        event.getRightClicked().addPassenger(player);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.hidePlayer(plugin, player);
        }
        player.setInvulnerable(true);
        player.setInvisible(true);
        player.setCollidable(false);
    }

    @EventHandler
    public void onEntityDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.getName().equals(ownerName)) return;
        if (!AbilityUtils.verifyAbilityCouple(this, event.getDismounted())) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showPlayer(plugin, player);
        }
        player.setInvulnerable(false);
        player.setInvisible(false);
        player.setCollidable(true);
    }

    public abstract void onTitanAbilityChargeUp(IronGolem titan);

    public abstract void onTitanAbilityActivate(IronGolem titan);

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (titan.get() != null) {
            IronGolem t = titan.get();
            if (t.isValid() && t.getPassengers().size() > 0) {
                event.setCancelled(true);
                if (event.getAction() != Action.PHYSICAL) {
                    if (actionCooldown.get() <= 0) {
                        pendingAction.set(event.getAction());
                        actionCooldown.set(ACTION_COOLDOWN);
                    }
                }
                return;
            }
        }

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;

        event.setCancelled(true);

        if (cooldown.isCoolingDown()) return;
        if (chargingUp.get()) return;
        if (abilityActive.get()) return;

        RayTraceResult result = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), 16, FluidCollisionMode.NEVER, true);
        if (result == null) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Too far away"));
            return;
        }
        Location spawnLocation = result.getHitPosition().toLocation(player.getWorld());

        abilityActive.set(true);

        AbilityUtils.consumeEnderPearl(this, player);
        EnderPearlAbilities.getInstance().emitEvent(
                EventListener.class,
                new AbilityActivateEvent(this),
                EventListener::onAbilityActivate
        );

        new FunctionChain(
                next -> AbilityUtils.chargeUpSequence(this, player, info.getChargeUp(), chargingUp, next),
                next -> {
                    if (titanControlRunnable != null && !titanControlRunnable.isCancelled())
                        titanControlRunnable.cancel();

                    titan.set(player.getWorld().spawn(spawnLocation.clone().add(0, 300, 0), IronGolem.class, entity -> {
                        entity.setInvisible(true);
                        entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.getCodeName(), ownerName)));
                    }));

                    WorldUtils.spawnParticleCubeOutline(spawnLocation.clone().subtract(1.5, 0, 1.5), spawnLocation.clone().add(1.5, 3, 1.5), Particle.END_ROD, 3, false);

                    pendingAction.set(null);
                    actionCooldown.set(ACTION_COOLDOWN);

                    Ability<?> ability = this;
                    (titanControlRunnable = new AbilityRunnable() {
                        boolean attackMode = false;
                        boolean landed = false;
                        boolean landBoosted = false;
                        IronGolem t;
                        BossBar abilityChargeUpBar;
                        int abilityChargeUp = 0;
                        AbilityCooldown abilityCooldown;

                        @Override
                        protected void start() {
                            super.start();
                            abilityCooldown = new AbilityCooldown(ability, player, false);
                            t = titan.get();
                            abilityChargeUpBar = Bukkit.createBossBar(ChatColor.WHITE + info.getTitanAbilityName(), BarColor.WHITE, BarStyle.SOLID);
                            abilityChargeUpBar.addPlayer(player);
                            abilityChargeUpBar.setProgress(0);
                        }

                        @Override
                        protected void tick() {
                            if (!landed) {
                                if (!landBoosted) {
                                    t.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, t.getLocation(), 1, 0.1, 0.1, 0.1, 0, null, true);
                                    RayTraceResult result = t.getWorld().rayTraceBlocks(t.getLocation(), new Vector(0, -1, 0), 50, FluidCollisionMode.ALWAYS, true);
                                    if (result != null && result.getHitBlock() != null) {
                                        for (int i = 0; i < 100; i++) {
                                            double angle = Math.random() * Math.PI * 2;
                                            double magnitude = Math.random() * 0.1 + 0.45;
                                            double x = Math.cos(angle);
                                            double z = Math.sin(angle);
                                            t.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, t.getLocation(), 0, x, 0, z, magnitude, null, true);
                                        }
                                        t.setInvisible(false);
                                        t.setVelocity(new Vector(0, -1, 0));
                                        t.getWorld().playSound(t.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1, 2);
                                        landBoosted = true;
                                    }
                                } else {
                                    t.getWorld().spawnParticle(Particle.SMOKE_LARGE, t.getLocation(), 1, 0.1, 0.1, 0.1, 0, null, true);
                                }
                                t.getWorld().playSound(t.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                                if (t.isOnGround()) {
                                    t.getWorld().playSound(t.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_HURT, 1, 0.5f);
                                    t.getWorld().playSound(t.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_HURT, 1, 0.5f);
                                    t.getWorld().playSound(t.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.2f, 0);
                                    t.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, t.getLocation(), 100, 0.5, 0.1, 0.5, 0.1);
                                    t.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, t.getLocation(), 100, 0.5, 0.1, 0.5, 0.03);
                                    landed = true;
                                }
                            }
                            if (!t.isValid()) {
                                cancel();
                                return;
                            }
                            if (!player.isValid()) {
                                t.eject();
                            }
                            if (t.getPassengers().size() <= 0) {
                                t.setAware(false);
                                abilityChargeUpBar.setVisible(false);
                                abilityChargeUp = 0;
                            } else {
                                t.setAware(true);
                                if (abilityCooldown.isCoolingDown()) {
                                    abilityChargeUpBar.setVisible(false);
                                } else if (abilityChargeUp > 0) {
                                    if (abilityChargeUp < info.getTitanAbilityChargeUp())
                                        abilityChargeUp++;
                                    abilityChargeUpBar.setVisible(true);
                                    if (abilityChargeUp >= 0 && abilityChargeUp <= info.getTitanAbilityChargeUp())
                                        abilityChargeUpBar.setProgress((double) abilityChargeUp / info.getTitanAbilityChargeUp());

                                    if (abilityChargeUp >= info.getTitanAbilityChargeUp()) {
                                        onTitanAbilityActivate(t);
                                        abilityChargeUp = 0;
                                        abilityCooldown.setCooldown(info.getTitanAbilityCooldown());
                                        abilityChargeUpBar.setProgress(0);
                                    } else {
                                        onTitanAbilityChargeUp(t);
                                    }
                                } else {
                                    abilityChargeUpBar.setVisible(false);
                                }
                                if (attackMode) {
                                    RayTraceResult result = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 48, entity -> {
                                        if (!(entity instanceof LivingEntity)) return false;
                                        if (entity instanceof Player p) {
                                            return !p.getName().equals(player.getName());
                                        }
                                        if (entity.equals(t)) return false;
                                        return true;
                                    });
                                    if (result != null)
                                        t.setTarget((LivingEntity) result.getHitEntity());
                                } else {
                                    if (t.isOnGround() && !t.isInWater())
                                        t.setVelocity(t.getVelocity().add(player.getLocation().getDirection().setY(0).multiply(0.08)));
                                    else
                                        t.setVelocity(t.getVelocity().add(player.getLocation().getDirection().setY(0).multiply(0.02)));
                                    t.setRotation(player.getLocation().getYaw(), t.getLocation().getPitch());
                                }
                            }
                            Action action = pendingAction.get();
                            if (action != null) {
                                if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                                    if (attackMode) {
                                        if (!abilityCooldown.isCoolingDown())
                                            if (abilityChargeUp <= 0)
                                                abilityChargeUp = 1;
                                    } else {
                                        if (t.isOnGround())
                                            t.setVelocity(t.getVelocity().add(new Vector(0, 1, 0)));
                                    }
                                } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                                    attackMode = !attackMode;
                                }
                                pendingAction.set(null);
                            }
                            if (actionCooldown.get() > 0)
                                actionCooldown.decrementAndGet();
                            if (t.getPassengers().size() <= 0) {
                                if (t.getTicksLived() % 20 == 0)
                                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Titan idle"));
                            } else {
                                String statusText = "";
                                statusText += attackMode ? "Attack mode" : "Move mode";
                                if (abilityCooldown.isCoolingDown())
                                    statusText += " | " + info.getTitanAbilityName() + " in " + abilityCooldown.getCooldownTicks() / 20 + "s";
                                if (t.getTarget() != null)
                                    statusText += " | Target: " + t.getTarget().getName();
                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(statusText));
                            }
                            if (abilityCooldown.isCoolingDown())
                                t.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, t.getLocation(), 2, 0.5, 0.5, 0.5, 0.02);
                            player.spawnParticle(Particle.ENCHANTMENT_TABLE, t.getLocation().add(0, 1, 0), 1, 0.8, 0.8, 0.8, 0.1);
                        }

                        @Override
                        protected void end() {
                            super.end();
                            if (t.isValid())
                                t.remove();
                            else {
                                t.getWorld().createExplosion(t.getLocation(), 2, false, false);
                                List<Entity> passengers = t.getPassengers();
                                t.eject();
                                for (Entity e : passengers) {
                                    e.setVelocity(e.getVelocity().add(new Vector(0, 3, 0)));
                                }
                            }
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                p.showPlayer(plugin, player);
                            }
                            player.setInvulnerable(false);
                            player.setInvisible(false);
                            player.setCollidable(true);
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(" "));
                            titan.set(null);
                        }
                    }).runTaskTimer(this, 0, 1);

                    abilityActive.set(false);
                }
        ).execute();
    }
}
