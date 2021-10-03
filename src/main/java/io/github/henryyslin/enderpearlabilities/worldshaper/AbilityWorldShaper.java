package io.github.henryyslin.enderpearlabilities.worldshaper;

import io.github.henryyslin.enderpearlabilities.Ability;
import io.github.henryyslin.enderpearlabilities.AbilityCooldown;
import io.github.henryyslin.enderpearlabilities.ActivationHand;
import io.github.henryyslin.enderpearlabilities.utils.*;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.concurrent.atomic.AtomicInteger;

public class AbilityWorldShaper implements Ability {
    static final int PROJECTILE_LIFETIME = 20;
    static final double PROJECTILE_SPEED = 4;

    public String getName() {
        return "World Shaper";
    }

    public String getOrigin() {
        return "Create Mod";
    }

    public String getConfigName() {
        return "worldshaper";
    }

    public String getDescription() {
        return "Fires an ender pearl which explodes on impact, instantly mining a 3x3 area of blocks using the tool held in main hand.";
    }

    public ActivationHand getActivation() {
        return ActivationHand.OffHand;
    }

    public int getChargeUp() {
        return 0;
    }

    public int getDuration() {
        return 0;
    }

    public int getCooldown() {
        return 20;
    }

    final Plugin plugin;
    final FileConfiguration config;
    final String ownerName;
    AbilityCooldown cooldown;
    AtomicInteger enderPearlHitTime = new AtomicInteger();

    public AbilityWorldShaper(Plugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
        this.ownerName = config.getString(getConfigName());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getName().equals(ownerName)) {
            cooldown = new AbilityCooldown(plugin, player);
            cooldown.startCooldown(getCooldown());
        }
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, getActivation())) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;
        if (PlayerUtils.getMainHandToolDurability(player).orElse(2) <= 1) return;

        AbilityUtils.relaunchEnderPearl(plugin, player, null, PROJECTILE_LIFETIME, PROJECTILE_SPEED);
        cooldown.startCooldown(getCooldown());
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();

        if (!(shooter instanceof Player player)) return;
        if (!projectile.hasMetadata("ability")) return;
        if (!player.getName().equals(ownerName)) return;

        if (projectile instanceof EnderPearl) {
            event.setCancelled(true);

            cooldown.startCooldown(getCooldown());
            enderPearlHitTime.set(player.getTicksLived());

            Entity hitEntity = event.getHitEntity();

            Location finalLocation;

            if (hitEntity == null) {
                // improve accuracy of the hit location
                finalLocation = AbilityUtils.fixProjectileHitLocation(player, projectile, PROJECTILE_SPEED);
            } else {
                finalLocation = hitEntity.getLocation();
            }

            finalLocation.setX(finalLocation.getBlockX() + 0.5);
            finalLocation.setY(finalLocation.getBlockY() + 0.5);
            finalLocation.setZ(finalLocation.getBlockZ() + 0.5);

            WorldUtils.spawnParticleRect(finalLocation.clone().add(-1.5, -1.5, -1.5), finalLocation.clone().add(1.5, 1.5, 1.5), Particle.VILLAGER_HAPPY, 5);

            int unbreaking = player.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.DURABILITY);

            player.getWorld().playSound(finalLocation, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1, 0);

            BlockUtils.getBlocks(finalLocation, 1).forEach(block -> {
                Material type = block.getType();
                if (type == Material.AIR) return;
                if (!PlayerUtils.canMainHandBreakBlock(player, block)) return;
                block.breakNaturally(player.getInventory().getItemInMainHand());

                if (type.isSolid()) {
                    if (player.getGameMode() != GameMode.CREATIVE) {
                        if (Math.random() * (unbreaking + 1) < 1) {
                            if (Math.random() < 0.5) {
                                ItemUtils.damageTool(player.getInventory().getItemInMainHand(), 1);
                            }
                        }
                    }
                }
            });

            if (player.getGameMode() != GameMode.CREATIVE) {
                if (Math.random() * 4 < 3) {
                    player.getWorld().dropItem(finalLocation, new ItemStack(Material.ENDER_PEARL, 1));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        if (Math.abs(event.getPlayer().getTicksLived() - enderPearlHitTime.get()) > 1) return;
        event.setCancelled(true);
    }
}
