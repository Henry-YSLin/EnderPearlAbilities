package io.github.henry_yslin.enderpearlabilities.abilities.titanfall;

import io.github.henry_yslin.enderpearlabilities.abilities.*;
import io.github.henry_yslin.enderpearlabilities.utils.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
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
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class TitanfallAbility extends Ability {

    static final int ACTION_COOLDOWN = 5;
    static final int ENERGY_SIPHON_COOLDOWN = 200;
    static final int ENERGY_SIPHON_CHARGE_UP = 20;

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        super.setConfigDefaults(config);
        config.addDefault("charge-up", 10);
        config.addDefault("duration", 0);
        config.addDefault("cooldown", 12000);
    }

    public TitanfallAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("titanfall")
                .name("Titanfall")
                .origin("Titanfall")
                .description("Deploy and pilot a titan for combat.\nTitan ability: Fire an electric blast that slows entity and regenerates health.\nPassive ability: dealing damage reduces titan cooldown.")
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
            cooldown.setCooldown(info.cooldown);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (player != null) {
            chargingUp.set(false);
            abilityActive.set(false);
            cooldown.setCooldown(info.cooldown);
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

        if (cooldown.isCoolingDown()) return;
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

                    Ability ability = this;
                    (titanControlRunnable = new AbilityRunnable() {
                        boolean attackMode = false;
                        boolean landed = false;
                        IronGolem t;
                        BossBar siphonChargeUpBar;
                        int siphonChargeUp = 0;
                        AbilityCooldown siphonCooldown;

                        @Override
                        protected void start() {
                            super.start();
                            siphonCooldown = new AbilityCooldown(ability, player, false);
                            t = titan.get();
                            siphonChargeUpBar = Bukkit.createBossBar(ChatColor.WHITE + "Energy Siphon", BarColor.WHITE, BarStyle.SOLID);
                            siphonChargeUpBar.addPlayer(player);
                            siphonChargeUpBar.setProgress(0);
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
                                siphonChargeUpBar.setVisible(false);
                                siphonChargeUp = 0;
                            } else {
                                t.setAware(true);
                                if (siphonCooldown.isCoolingDown()) {
                                    siphonChargeUpBar.setVisible(false);
                                } else if (siphonChargeUp > 0) {
                                    if (siphonChargeUp < ENERGY_SIPHON_CHARGE_UP)
                                        siphonChargeUp++;
                                    siphonChargeUpBar.setVisible(true);
                                    siphonChargeUpBar.setProgress((double) siphonChargeUp / ENERGY_SIPHON_CHARGE_UP);
                                    if (siphonChargeUp >= ENERGY_SIPHON_CHARGE_UP) {
                                        RayTraceResult result = player.getWorld().rayTrace(player.getEyeLocation(), player.getEyeLocation().getDirection(), 32, FluidCollisionMode.NEVER, true, 0, entity -> !entity.equals(player) && !entity.equals(t) && entity instanceof LivingEntity);
                                        Vector hit;
                                        boolean effective = false;
                                        if (result == null) {
                                            hit = player.getEyeLocation().toVector().add(player.getEyeLocation().getDirection().multiply(32));
                                        } else {
                                            hit = result.getHitPosition();
                                            LivingEntity entity = (LivingEntity) result.getHitEntity();
                                            if (entity != null) {
                                                effective = true;
                                                entity.damage(4, player);
                                                entity.addPotionEffect(PotionEffectType.SLOW.createEffect(60, 1));
                                                t.setHealth(Math.min(EntityUtils.getMaxHealth(t), t.getHealth() + 4));
                                            }
                                        }
                                        WorldUtils.spawnParticleLine(t.getEyeLocation(), hit.toLocation(t.getWorld()), effective ? Particle.DRAGON_BREATH : Particle.ELECTRIC_SPARK, 3, false);
                                        siphonChargeUp = 0;
                                        siphonCooldown.setCooldown(ENERGY_SIPHON_COOLDOWN);
                                        siphonChargeUpBar.setProgress(0);
                                    }
                                } else {
                                    siphonChargeUpBar.setVisible(false);
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
                                        if (!siphonCooldown.isCoolingDown())
                                            if (siphonChargeUp <= 0)
                                                siphonChargeUp = 1;
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
                                if (siphonCooldown.isCoolingDown())
                                    statusText += " | Energy Siphon in " + siphonCooldown.getCooldownTicks() / 20 + "s";
                                if (t.getTarget() != null)
                                    statusText += " | Target: " + t.getTarget().getName();
                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(statusText));
                            }
                            if (siphonCooldown.isCoolingDown())
                                t.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, t.getLocation(), 2, 0.5, 0.5, 0.5, 0.02);
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
