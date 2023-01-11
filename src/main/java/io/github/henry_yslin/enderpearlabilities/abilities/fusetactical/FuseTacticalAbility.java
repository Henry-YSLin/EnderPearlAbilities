package io.github.henry_yslin.enderpearlabilities.abilities.fusetactical;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityCooldown;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.MultipleChargeCooldown;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.ProjectileUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.concurrent.atomic.AtomicBoolean;

public class FuseTacticalAbility extends Ability<FuseTacticalAbilityInfo> {

    static final int PROJECTILE_LIFETIME = 100;
    static final double PROJECTILE_SPEED = 3;
    static final boolean PROJECTILE_GRAVITY = true;
    static final double EXPLOSION_RADIUS = 2.5;

    public FuseTacticalAbility(Plugin plugin, FuseTacticalAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);

    @Override
    protected AbilityCooldown createCooldown() {
        return new MultipleChargeCooldown(this, player, info.getCharge());
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

    @Override
    public void onDisable() {
        super.onDisable();
    }

    private void setUpPlayer(Player player) {
        cooldown.setCooldown(info.getCooldown());
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;

        event.setCancelled(true);

        if (!cooldown.isAbilityUsable()) return;
        if (chargingUp.get()) return;

        new FunctionChain(
                next -> AbilityUtils.chargeUpSequence(this, player, info.getChargeUp(), chargingUp, next),
                next -> {
                    Projectile projectile = AbilityUtils.fireProjectile(this, player, null, PROJECTILE_LIFETIME, PROJECTILE_SPEED, PROJECTILE_GRAVITY);
                    if (projectile != null) {
                        cooldown.setCooldown(info.getCooldown());
                        AbilityUtils.consumeEnderPearl(this, player);
                        EnderPearlAbilities.getInstance().emitEvent(
                                EventListener.class,
                                new AbilityActivateEvent(this),
                                EventListener::onAbilityActivate
                        );
                    }
                }
        ).execute();
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();

        if (!(shooter instanceof Player shooterPlayer)) return;
        if (!AbilityUtils.verifyAbilityCouple(this, projectile)) return;
        if (!shooterPlayer.getName().equals(ownerName)) return;

        if (!(projectile instanceof Snowball)) return;

        event.setCancelled(true);

        projectile.getWorld().spawnParticle(Particle.SMOKE_NORMAL, projectile.getLocation(), 10, 0.1, 0.1, 0.1, 0.02);

        Location hitPosition = ProjectileUtils.correctProjectileHitLocation(projectile).add(0, 0.5, 0);

        new AbilityRunnable() {
            @Override
            protected void start() {
                projectile.getWorld().playSound(projectile.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 2);
            }

            @Override
            protected void tick() {
                projectile.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, hitPosition, 5, EXPLOSION_RADIUS / 2, EXPLOSION_RADIUS / 2, EXPLOSION_RADIUS / 2, 0);
                projectile.getWorld().playSound(projectile.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.2f, (float) Math.random() * 2);
                for (Entity entity : projectile.getWorld().getNearbyEntities(hitPosition, EXPLOSION_RADIUS, EXPLOSION_RADIUS, EXPLOSION_RADIUS)) {
                    if (entity instanceof LivingEntity livingEntity) {
                        livingEntity.damage(1, player);
                        livingEntity.setNoDamageTicks(Math.min(livingEntity.getNoDamageTicks(), 10));
                    }
                }
            }

            @Override
            protected void end() {
                super.end();
            }
        }.runTaskRepeated(this, 0, 1, info.getDuration());

        projectile.remove();
    }
}
