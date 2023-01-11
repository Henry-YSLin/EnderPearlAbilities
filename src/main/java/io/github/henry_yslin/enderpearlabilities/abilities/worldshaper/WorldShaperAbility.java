package io.github.henry_yslin.enderpearlabilities.abilities.worldshaper;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityCooldown;
import io.github.henry_yslin.enderpearlabilities.abilities.MultipleChargeCooldown;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.utils.*;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.concurrent.atomic.AtomicBoolean;

public class WorldShaperAbility extends Ability<WorldShaperAbilityInfo> {

    static final int PROJECTILE_LIFETIME = 30;
    static final double PROJECTILE_SPEED = 4;

    public WorldShaperAbility(Plugin plugin, WorldShaperAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);

    @Override
    protected AbilityCooldown createCooldown() {
        return new MultipleChargeCooldown(this, player, 5);
    }

    @Override
    public boolean isActive() {
        return false;
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
            cooldown.setCooldown(info.getCooldown());
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (player != null) {
            chargingUp.set(false);
            cooldown.setCooldown(info.getCooldown());
        }
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;

        event.setCancelled(true);

        if (!cooldown.isAbilityUsable()) return;
        if (PlayerUtils.getMainHandToolDurability(player).orElse(2) <= 1) return;

        new FunctionChain(
                next -> AbilityUtils.chargeUpSequence(this, player, info.getChargeUp(), chargingUp, next),
                next -> {
                    AbilityUtils.fireProjectile(this, player, null, PROJECTILE_LIFETIME, PROJECTILE_SPEED, false);
                    cooldown.setCooldown(info.getCooldown());
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

        AbilityUtils.consumeEnderPearl(this, player);
        EnderPearlAbilities.getInstance().emitEvent(
                EventListener.class,
                new AbilityActivateEvent(this),
                EventListener::onAbilityActivate
        );

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
                        ItemStackUtils.damageTool(player.getInventory().getItemInMainHand(), 1);
                    }
                }
            }
        });

        // This is disabled because world shaper does not consume ender pearl
/*
        if (player.getGameMode() != GameMode.CREATIVE) {
            if (Math.random() * 4 < 3) {
                player.getWorld().dropItem(finalLocation, new ItemStack(Material.ENDER_PEARL, 1));
            }
        }
*/
    }
}
