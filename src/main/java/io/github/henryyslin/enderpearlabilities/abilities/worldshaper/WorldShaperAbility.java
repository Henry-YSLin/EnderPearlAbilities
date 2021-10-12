package io.github.henryyslin.enderpearlabilities.abilities.worldshaper;

import io.github.henryyslin.enderpearlabilities.abilities.Ability;
import io.github.henryyslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henryyslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henryyslin.enderpearlabilities.utils.*;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
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

public class WorldShaperAbility extends Ability {
    static final int PROJECTILE_LIFETIME = 30;
    static final double PROJECTILE_SPEED = 4;

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 0);
        config.addDefault("cooldown", 20);
    }

    public WorldShaperAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("worldshaper")
                .name("World Shaper")
                .origin("Create Mod")
                .description("Fires an ender pearl which explodes on impact, instantly mining a 3x3 area of blocks using the tool held in main hand.")
                .activation(ActivationHand.OffHand);

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

    final AtomicInteger enderPearlHitTime = new AtomicInteger();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        super.onPlayerJoin(event);
        Player player = event.getPlayer();
        if (player.getName().equals(ownerName)) {
            cooldown.startCooldown(info.cooldown);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (player != null) {
            cooldown.startCooldown(info.cooldown);
        }
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;
        if (PlayerUtils.getMainHandToolDurability(player).orElse(2) <= 1) return;

        new FunctionChain(
                next -> AbilityUtils.chargeUpSequence(this, player, info.chargeUp, next),
                next -> {
                    AbilityUtils.fireEnderPearl(this, player, null, PROJECTILE_LIFETIME, PROJECTILE_SPEED, false);
                    cooldown.startCooldown(info.cooldown);
                }
        ).execute();
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();

        if (!(shooter instanceof Player player)) return;
        if (!AbilityUtils.verifyAbilityCouple(this, projectile)) return;
        if (!player.getName().equals(ownerName)) return;

        if (!(projectile instanceof EnderPearl)) return;

        event.setCancelled(true);

        projectile.remove();
        cooldown.startCooldown(info.cooldown);
        enderPearlHitTime.set(player.getTicksLived());

        Entity hitEntity = event.getHitEntity();

        Location finalLocation;

        if (hitEntity == null) {
            // improve accuracy of the hit location
            finalLocation = AbilityUtils.correctProjectileHitLocation(projectile).add(projectile.getVelocity().normalize().multiply(0.1));
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

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        if (Math.abs(event.getPlayer().getTicksLived() - enderPearlHitTime.get()) > 1) return;
        event.setCancelled(true);
    }
}
