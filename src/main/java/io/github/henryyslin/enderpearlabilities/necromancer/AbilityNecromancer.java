package io.github.henryyslin.enderpearlabilities.necromancer;

import io.github.henryyslin.enderpearlabilities.Ability;
import io.github.henryyslin.enderpearlabilities.AbilityCooldown;
import io.github.henryyslin.enderpearlabilities.ActivationHand;
import io.github.henryyslin.enderpearlabilities.utils.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
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

public class AbilityNecromancer implements Ability {
    public String getName() {
        return "Skeleton Army";
    }

    public String getOrigin() {
        return "Clash Royale";
    }

    public String getConfigName() {
        return "necromancer";
    }

    public String getDescription() {
        return "Summon skeletons to fight whatever the summoner is looking at. Summoned skeletons obey commands until death.\nPassive ability: No skeletons will ever actively attack the summoner.";
    }

    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    public int getChargeUp() {
        return 60;
    }

    public int getDuration() {
        return 100;
    }

    public int getCooldown() {
        return 20;
    }

    final Plugin plugin;
    final FileConfiguration config;
    final String ownerName;
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    final AtomicReference<LivingEntity> playerTarget = new AtomicReference<>();
    final List<Skeleton> slaves = new ArrayList<>();
    AbilityCooldown cooldown;
    PlayerTargetTracker playerTargetTracker;

    public AbilityNecromancer(Plugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
        this.ownerName = config.getString(getConfigName());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getName().equals(ownerName)) {
            abilityActive.set(false);
            cooldown = new AbilityCooldown(plugin, player);
            cooldown.startCooldown(getCooldown());
            if (playerTargetTracker != null && !playerTargetTracker.isCancelled())
                playerTargetTracker.cancel();
            playerTargetTracker = new PlayerTargetTracker(player, slaves, playerTarget);
            playerTargetTracker.runTaskTimer(plugin, 0, 10);
        }
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
                if (skeleton.hasMetadata("ability") && target.hasMetadata("ability"))
                    event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Skeleton skeleton)) return;
        if (skeleton.hasMetadata("ability"))
            event.getEntity().setTicksLived(1000);
    }

    @EventHandler
    public void onPlayerClicks(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, getActivation())) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;
        if (abilityActive.get()) return;

        new FunctionChain(
                next -> {
                    cooldown.startCooldown(getCooldown());
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 1, 0);
                    if (player.getGameMode() != GameMode.CREATIVE) {
                        player.getInventory().removeItem(new ItemStack(Material.ENDER_PEARL, 1));
                    }
                    next.invoke();
                },
                next -> AbilityUtils.chargeUpSequence(plugin, player, getChargeUp(), next),
                next -> new AdvancedRunnable() {
                    BossBar bossbar;
                    List<Block> spawnLocations;

                    @Override
                    protected synchronized void start() {
                        abilityActive.set(true);
                        player.setCooldown(Material.ENDER_PEARL, getDuration());
                        bossbar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + getName(), BarColor.PURPLE, BarStyle.SOLID);
                        bossbar.addPlayer(player);
                        spawnLocations = BlockUtils.getSafeSpawningBlocks(player.getLocation(), 10);

                        if (spawnLocations.isEmpty()) {
                            abilityActive.set(false);
                            cooldown.cancelCooldown();
                            player.sendMessage(ChatColor.RED + "No place to summon slaves!");
                            plugin.getLogger().info("[" + getName() + "] No valid spawn locations at " + player.getLocation() + ". Cancelling.");
                            cancel();
                        }
                    }

                    @Override
                    protected synchronized void tick() {
                        if (!abilityActive.get()) {
                            cancel();
                        }
                        bossbar.setProgress(count / (double) getDuration() * 10);
                        Location spawnLocation = ListUtils.getRandom(spawnLocations).getLocation().add(0, -1, 0);
                        Skeleton skeleton = (Skeleton) player.getWorld().spawnEntity(spawnLocation, EntityType.SKELETON);
                        skeleton.setAI(false);
                        skeleton.setMetadata("ability", new FixedMetadataValue(plugin, ownerName));
                        skeleton.setCustomName(ownerName + "'s slave");
                        skeleton.setCustomNameVisible(true);
                        skeleton.setPersistent(false);
                        if (WorldUtils.isDaytime(skeleton.getWorld())) {
                            if (Math.random() < 0.5) {
                                EntityEquipment equipment = skeleton.getEquipment();
                                if (equipment != null)
                                    equipment.setHelmet(new ItemStack(Material.IRON_HELMET, 1));
                            }
                        }
                        slaves.add(skeleton);
                        new SlaveSpawning(plugin, player, skeleton, playerTarget).runTaskRepeated(plugin, 0, 2, 11);
                    }

                    @Override
                    protected synchronized void end() {
                        bossbar.removeAll();
                        if (abilityActive.get()) {
                            abilityActive.set(false);
                            cooldown.startCooldown(getCooldown());
                            next.invoke();
                        }
                    }
                }.runTaskRepeated(plugin, 0, 10, getDuration() / 10)
        ).execute();
    }
}
