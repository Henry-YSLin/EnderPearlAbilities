package io.github.henry_yslin.enderpearlabilities.abilities.titanfall;

import io.github.henry_yslin.enderpearlabilities.abilities.*;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.PlayerUtils;
import io.github.henry_yslin.enderpearlabilities.utils.WorldUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class TitanfallAbility extends Ability {

    static final int ACTION_COOLDOWN = 5;

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        super.setConfigDefaults(config);
        config.addDefault("charge-up", 10);
        config.addDefault("duration", 0);
        config.addDefault("cooldown", 2400);
    }

    public TitanfallAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("titanfall")
                .name("Titanfall")
                .origin("Titanfall")
                .description("Deploy and pilot a titan for combat.")
                .usage("Right click to summon a titan at your location. Use vehicle controls to mount and dismount the titan. While mounted, right click to switch between attack and move mode. Left click an entity in attack mode to lock target. Left click in move mode to jump. You are invincible while controlling the titan, but you will be ejected upwards when the titan is destroyed.")
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
    final AtomicReference<Action> pendingAction = new AtomicReference<>();
    final AtomicInteger actionCooldown = new AtomicInteger(0);
    final AtomicReference<IronGolem> titan = new AtomicReference<>();
    AbilityRunnable titanControlRunnable;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        super.onPlayerJoin(event);
        Player player = event.getPlayer();
        if (player.getName().equals(ownerName)) {
            chargingUp.set(false);
            abilityActive.set(false);
            cooldown.startCooldown(info.cooldown);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (player != null) {
            chargingUp.set(false);
            abilityActive.set(false);
            cooldown.startCooldown(info.cooldown);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!player.getName().equals(ownerName)) return;
        if (!AbilityUtils.verifyAbilityCouple(this, event.getRightClicked())) return;
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

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;
        if (chargingUp.get()) return;
        if (abilityActive.get()) return;

        abilityActive.set(true);

        PlayerUtils.consumeEnderPearl(player);

        new FunctionChain(
                next -> AbilityUtils.chargeUpSequence(this, player, info.chargeUp, chargingUp, next),
                next -> {
                    if (titanControlRunnable != null && !titanControlRunnable.isCancelled())
                        titanControlRunnable.cancel();

                    titan.set(player.getWorld().spawn(player.getLocation().add(0, 300, 0), IronGolem.class, entity -> entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.codeName, ownerName)))));

                    WorldUtils.spawnParticleCubeOutline(player.getLocation().subtract(1.5, 0, 1.5), player.getLocation().add(1.5, 3, 1.5), Particle.END_ROD, 3, false);

                    pendingAction.set(null);
                    actionCooldown.set(ACTION_COOLDOWN);

                    (titanControlRunnable = new AbilityRunnable() {
                        boolean attackMode = false;
                        boolean landed = false;
                        IronGolem t;

                        @Override
                        protected void start() {
                            super.start();
                            t = titan.get();
                        }

                        @Override
                        protected void tick() {
                            if (!landed && t.isOnGround()) {
                                t.getWorld().playSound(t.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_HURT, 1, 0.5f);
                                t.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, t.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                                landed = true;
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
                            } else {
                                t.setAware(true);
                                if (!attackMode) {
                                    if (t.isOnGround() && !t.isInWater())
                                        t.setVelocity(t.getVelocity().add(player.getLocation().getDirection().setY(0).multiply(0.08)));
                                    else
                                        t.setVelocity(t.getVelocity().add(player.getLocation().getDirection().setY(0).multiply(0.02)));
                                    t.setRotation(player.getLocation().getYaw(), player.getLocation().getPitch());
                                }
                            }
                            Action action = pendingAction.get();
                            if (action != null) {
                                if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                                    if (attackMode) {
                                        LivingEntity target = PlayerUtils.getPlayerTargetLivingEntity(player);
                                        if (!Objects.equals(target, t))
                                            t.setTarget(target);
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
                            if (t.getPassengers().size() > 0)
                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(attackMode ? "Attack mode" : "Move mode"));
                            else
                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Dismounted"));
                            t.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, t.getLocation().add(0, 1, 0), 1, 0.8, 0.8, 0.8, 0.1);
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
