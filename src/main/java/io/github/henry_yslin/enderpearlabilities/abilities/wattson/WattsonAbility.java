package io.github.henry_yslin.enderpearlabilities.abilities.wattson;

import io.github.henry_yslin.enderpearlabilities.abilities.*;
import io.github.henry_yslin.enderpearlabilities.utils.*;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
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

public class WattsonAbility extends Ability {

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 20);
        config.addDefault("duration", 1000);
        config.addDefault("cooldown", 800);
    }

    public WattsonAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("wattson")
                .name("Interception Pylon")
                .origin("Apex - Wattson")
                .description("Place an electrified pylon that destroys incoming projectiles and repairs damaged armors.")
                .usage("Right click on a block to place down the pylon. It will then intercept all projectiles and primed explosives in range that are not fired by the player who owns the pylon. It will also regenerate armor durability for all players in range.")
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

    final AtomicBoolean abilityActive = new AtomicBoolean(false);

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
            cooldown.startCooldown(info.cooldown);
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
            cooldown.startCooldown(info.cooldown);
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
        event.getEntity().getWorld().createExplosion(event.getEntity().getLocation(), 3, false, false);
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;

        if (abilityActive.get()) return;

        AtomicReference<EnderCrystal> pylon = new AtomicReference<>();
        World world = player.getWorld();

        if (!event.getClickedBlock().getType().isSolid()) return;
        Location location = event.getClickedBlock().getLocation();
        location.add(0.5, 2, 0.5);
        if (location.getBlock().getType() != Material.AIR) return;

        abilityActive.set(true);
        PlayerUtils.consumeEnderPearl(player);

        new FunctionChain(
                next -> {
                    EnderCrystal crystal = world.spawn(location, EnderCrystal.class, false, entity -> {
                        entity.setGravity(false);
                        entity.setGlowing(true);
                        entity.setShowingBottom(true);
                        entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.codeName, ownerName)));
                    });
                    pylon.set(crystal);

                    WorldUtils.spawnParticleRect(location.clone().add(-8, -8, -8), location.clone().add(8, 8, 8), Particle.END_ROD, 5, true);
                    world.playSound(crystal.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 0.5f);

                    next.run();
                },
                next -> new AbilityRunnable() {
                    @Override
                    protected void tick() {
                        world.spawnParticle(Particle.WHITE_ASH, pylon.get().getLocation(), 4, 0.5, 0.5, 0.5, 0.02);
                    }

                    @Override
                    protected void end() {
                        if (this.hasCompleted())
                            next.run();
                        else
                            pylon.get().remove();
                    }
                }.runTaskRepeated(this, 0, 2, info.chargeUp / 2),
                next -> new AbilityRunnable() {
                    BossBar bossbar;
                    int beamTick;

                    @Override
                    protected synchronized void start() {
                        bossbar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.name, BarColor.PURPLE, BarStyle.SOLID);
                        bossbar.addPlayer(player);
                    }

                    @Override
                    protected synchronized void tick() {
                        EnderCrystal crystal = pylon.get();
                        if (!abilityActive.get() || !crystal.isValid()) {
                            cancel();
                            return;
                        }
                        bossbar.setProgress(count / (double) info.duration);
                        if (count % 40 == 0)
                            world.playSound(crystal.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1, 0.5f);
                        if (count % 5 == 0)
                            world.spawnParticle(Particle.END_ROD, crystal.getLocation(), 2, 0.5, 0.5, 0.5, 0.02);
                        if (beamTick <= 0) crystal.setBeamTarget(null);
                        else beamTick--;
                        boolean intercepted = false;
                        for (Entity entity : world.getNearbyEntities(crystal.getLocation(), 8, 8, 8)) {
                            if (shouldIntercept(entity) && !intercepted) {
                                crystal.setBeamTarget(entity.getLocation().add(0, -2, 0));
                                beamTick = 5;
                                world.spawnParticle(Particle.SMOKE_LARGE, entity.getLocation(), 5, 0.1, 0.1, 0.1, 0.01);
                                world.playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.1f, 0);
                                entity.remove();
                                intercepted = true;
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
                        cooldown.startCooldown(info.cooldown);
                        next.run();
                    }
                }.runTaskRepeated(this, 0, 1, info.duration)
        ).execute();
    }
}
