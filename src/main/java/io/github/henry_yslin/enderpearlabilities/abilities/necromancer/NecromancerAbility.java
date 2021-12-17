package io.github.henry_yslin.enderpearlabilities.abilities.necromancer;

import io.github.henry_yslin.enderpearlabilities.abilities.*;
import io.github.henry_yslin.enderpearlabilities.utils.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class NecromancerAbility extends Ability {

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        super.setConfigDefaults(config);
        config.addDefault("charge-up", 60);
        config.addDefault("duration", 100);
        config.addDefault("cooldown", 1800);
    }

    public NecromancerAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("necromancer")
                .name("Skeleton Army")
                .origin("Clash Royale")
                .description("Summon skeletons to fight whatever the player is looking at. Summoned skeletons obey commands until death.\nPassive ability: No skeletons will ever actively attack the player.")
                .usage("Right click to summon skeletons. They target the entity that is currently under the player's crosshair. If the skeletons are summoned during daytime, they have a higher chance of wearing helmets.")
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
    final AtomicReference<LivingEntity> playerTarget = new AtomicReference<>();
    final List<Skeleton> slaves = new ArrayList<>();
    PlayerTargetTracker playerTargetTracker;

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
        if (playerTargetTracker != null && !playerTargetTracker.isCancelled())
            playerTargetTracker.cancel();
        playerTargetTracker = new PlayerTargetTracker(player, () -> {
            slaves.removeIf(skeleton -> !skeleton.isValid());
            return !slaves.isEmpty();
        }, playerTarget);
        playerTargetTracker.runTaskTimer(this, 0, 10);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Skeleton skeleton) {
            if (event.getTarget() instanceof Player player) {
                if (!player.getName().equals(ownerName)) return;
                player.getWorld().spawnParticle(Particle.SOUL, player.getLocation(), 2, 0.5, 0.5, 0.5, 0.02);
                event.setCancelled(true);
            } else if (event.getTarget() instanceof Skeleton target) {
                if (AbilityUtils.verifyAbilityCouple(this, skeleton) && AbilityUtils.verifyAbilityCouple(this, target))
                    event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Skeleton skeleton)) return;
        if (AbilityUtils.verifyAbilityCouple(this, skeleton))
            event.getEntity().setTicksLived(1000);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;
        if (chargingUp.get()) return;
        if (abilityActive.get()) return;

        new FunctionChain(
                next -> {
                    cooldown.startCooldown(info.cooldown);
                    PlayerUtils.consumeEnderPearl(player);
                    next.run();
                },
                next -> AbilityUtils.chargeUpSequence(this, player, info.chargeUp, chargingUp, next),
                next -> new AbilityRunnable() {
                    BossBar bossbar;
                    List<Block> spawnLocations;

                    @Override
                    protected synchronized void start() {
                        abilityActive.set(true);
                        player.setCooldown(Material.ENDER_PEARL, info.duration);
                        bossbar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.name, BarColor.PURPLE, BarStyle.SOLID);
                        bossbar.addPlayer(player);
                        spawnLocations = BlockUtils.getBlocks(player.getLocation(), 10, BlockUtils::isSafeSpawningBlock);

                        if (spawnLocations.isEmpty()) {
                            abilityActive.set(false);
                            cooldown.cancelCooldown();
                            player.sendMessage(ChatColor.RED + "No place to summon slaves!");
                            plugin.getLogger().info("[" + info.name + "] No valid spawn locations at " + player.getLocation() + ". Cancelling.");
                            cancel();
                        }
                    }

                    @Override
                    protected synchronized void tick() {
                        if (!abilityActive.get() || !player.isValid()) {
                            cancel();
                        }
                        bossbar.setProgress(count / (double) info.duration * 10);
                        Location spawnLocation = ListUtils.getRandom(spawnLocations).getLocation().add(0.5, -1, 0.5);
                        Skeleton skeleton = player.getWorld().spawn(spawnLocation, Skeleton.class, true, entity -> {
                            entity.setAI(false);
                            entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.codeName, ownerName)));
                            entity.setCustomName(ownerName + "'s slave");
                            entity.setCustomNameVisible(true);
                            entity.setPersistent(false);
                            if (WorldUtils.isDaytime(player.getWorld())) {
                                if (Math.random() < 0.5) {
                                    EntityEquipment equipment = entity.getEquipment();
                                    if (equipment != null && (equipment.getHelmet() == null || equipment.getHelmet().getType() == Material.AIR))
                                        equipment.setHelmet(new ItemStack(Material.IRON_HELMET, 1));
                                }
                            }
                        });
                        slaves.add(skeleton);
                        skeleton.getWorld().playSound(skeleton.getLocation(), Sound.ENTITY_SKELETON_HORSE_AMBIENT, 1, 0);
                        new SlaveSpawning(player, skeleton, playerTarget).runTaskRepeated(executor, 0, 2, 11);
                    }

                    @Override
                    protected synchronized void end() {
                        bossbar.removeAll();
                        if (abilityActive.get()) {
                            abilityActive.set(false);
                            if (this.hasCompleted())
                                cooldown.startCooldown(info.cooldown);
                            else {
                                for (Skeleton skeleton : slaves) {
                                    skeleton.setCustomName(null);
                                    skeleton.setCustomNameVisible(false);
                                    skeleton.removeMetadata("ability", executor.plugin);
                                    skeleton.setAI(true);
                                }
                            }
                            next.run();
                        }
                    }
                }.runTaskRepeated(this, 0, 10, info.duration / 10)
        ).execute();
    }
}
