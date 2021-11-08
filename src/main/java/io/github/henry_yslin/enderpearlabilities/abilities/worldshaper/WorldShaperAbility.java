package io.github.henry_yslin.enderpearlabilities.abilities.worldshaper;

import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henry_yslin.enderpearlabilities.utils.*;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.concurrent.atomic.AtomicBoolean;

public class WorldShaperAbility extends Ability {

    static final int PROJECTILE_LIFETIME = 30;
    static final double PROJECTILE_SPEED = 4;

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        super.setConfigDefaults(config);
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
                .description("Fires a projectile which explodes on impact, instantly mining a 3x3 area of blocks using the tool held in main hand.")
                .usage("Right click to fire. The tool held in main hand will be used to mine a 3x3 area where the projectile lands. Blocks inside this blast area will not be mined if the tool in main hand cannot produce block drops from those blocks. Tool durability is twice as efficient as manual mining and will not completely deplete while using this ability. There is a high chance for the ender pearl to drop as item after the blast.")
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

    final AtomicBoolean chargingUp = new AtomicBoolean(false);

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        super.onPlayerJoin(event);
        Player player = event.getPlayer();
        if (player.getName().equals(ownerName)) {
            chargingUp.set(false);
            cooldown.startCooldown(info.cooldown);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (player != null) {
            chargingUp.set(false);
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
                next -> {
                    PlayerUtils.consumeEnderPearl(player);
                    next.run();
                },
                next -> AbilityUtils.chargeUpSequence(this, player, info.chargeUp, chargingUp, next),
                next -> {
                    AbilityUtils.fireProjectile(this, player, null, PROJECTILE_LIFETIME, PROJECTILE_SPEED, false);
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

        if (!(projectile instanceof Snowball)) return;

        event.setCancelled(true);

        projectile.remove();

        Entity hitEntity = event.getHitEntity();

        Location finalLocation;

        if (hitEntity == null) {
            // improve accuracy of the hit location
            finalLocation = ProjectileUtils.correctProjectileHitLocation(projectile).add(projectile.getVelocity().normalize().multiply(0.1));
        } else {
            finalLocation = hitEntity.getLocation();
        }

        finalLocation.setX(finalLocation.getBlockX() + 0.5);
        finalLocation.setY(finalLocation.getBlockY() + 0.5);
        finalLocation.setZ(finalLocation.getBlockZ() + 0.5);

        WorldUtils.spawnParticleCubeOutline(finalLocation.clone().add(-1.5, -1.5, -1.5), finalLocation.clone().add(1.5, 1.5, 1.5), Particle.VILLAGER_HAPPY, 5, true);
        projectile.getWorld().createExplosion(projectile.getLocation(), 1, false, false);

        int unbreaking = player.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.DURABILITY);

        BlockUtils.getBlocks(finalLocation, 1).forEach(block -> {
            Material type = block.getType();
            if (type == Material.AIR) return;
            if (!PlayerUtils.canMainHandBreakBlock(player, block)) return;
            block.breakNaturally(player.getInventory().getItemInMainHand());

            if (type.isSolid()) {
                if (player.getGameMode() != GameMode.CREATIVE) {
                    if (Math.random() * (unbreaking + 1) < 1) {
                        if (Math.random() < 0.5) {
                            ItemStackUtils.damageTool(player.getInventory().getItemInMainHand(), 1);
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
