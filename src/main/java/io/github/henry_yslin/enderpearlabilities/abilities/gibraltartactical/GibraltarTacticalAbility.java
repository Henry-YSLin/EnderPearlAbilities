package io.github.henry_yslin.enderpearlabilities.abilities.gibraltartactical;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.managers.shield.EntityBlockingShieldBehavior;
import io.github.henry_yslin.enderpearlabilities.managers.shield.Shield;
import io.github.henry_yslin.enderpearlabilities.managers.shield.ShieldManager;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class GibraltarTacticalAbility extends Ability<GibraltarTacticalAbilityInfo> {

    static final int PROJECTILE_LIFETIME = 100;
    static final double PROJECTILE_SPEED = 0.5;
    static final boolean PROJECTILE_GRAVITY = true;
    static final double SHIELD_RADIUS = 6;

    public GibraltarTacticalAbility(Plugin plugin, GibraltarTacticalAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean blockShoot = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);

    @Override
    public boolean isActive() {
        return abilityActive.get();
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

    private void setUpPlayer(Player player) {
        abilityActive.set(false);
        blockShoot.set(false);
        cooldown.setCooldown(info.getCooldown());
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;

        event.setCancelled(true);

        if (cooldown.isCoolingDown()) return;
        if (chargingUp.get()) return;
        if (abilityActive.get()) return;

        new FunctionChain(
                next -> {
                    AbilityUtils.consumeEnderPearl(this, player);
                    EnderPearlAbilities.getInstance().emitEvent(
                            EventListener.class,
                            new AbilityActivateEvent(this),
                            EventListener::onAbilityActivate
                    );
                    next.run();
                },
                next -> AbilityUtils.chargeUpSequence(this, player, info.getChargeUp(), chargingUp, next),
                next -> AbilityUtils.fireProjectile(this, player, blockShoot, PROJECTILE_LIFETIME, PROJECTILE_SPEED, PROJECTILE_GRAVITY)
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
        abilityActive.set(true);
        blockShoot.set(false);

        Location finalLocation = projectile.getLocation();

        World world = projectile.getWorld();

        List<Shield> shields = new ArrayList<>(5);

        shields.add(new Shield(
                world,
                BoundingBox.of(finalLocation.clone().add(-SHIELD_RADIUS, -1.5, SHIELD_RADIUS - 0.005), finalLocation.clone().add(SHIELD_RADIUS, SHIELD_RADIUS, SHIELD_RADIUS + 0.005)),
                new Vector(0, 0, 1),
                EntityBlockingShieldBehavior.getInstance()
        ));
        shields.add(new Shield(
                world,
                BoundingBox.of(finalLocation.clone().add(-SHIELD_RADIUS, -1.5, -SHIELD_RADIUS - 0.005), finalLocation.clone().add(SHIELD_RADIUS, SHIELD_RADIUS, -SHIELD_RADIUS + 0.005)),
                new Vector(0, 0, -1),
                EntityBlockingShieldBehavior.getInstance()
        ));
        shields.add(new Shield(
                world,
                BoundingBox.of(finalLocation.clone().add(SHIELD_RADIUS - 0.005, -1.5, -SHIELD_RADIUS), finalLocation.clone().add(SHIELD_RADIUS + 0.005, SHIELD_RADIUS, SHIELD_RADIUS)),
                new Vector(1, 0, 0),
                EntityBlockingShieldBehavior.getInstance()
        ));
        shields.add(new Shield(
                world,
                BoundingBox.of(finalLocation.clone().add(-SHIELD_RADIUS - 0.005, -1.5, -SHIELD_RADIUS), finalLocation.clone().add(-SHIELD_RADIUS + 0.005, SHIELD_RADIUS, SHIELD_RADIUS)),
                new Vector(-1, 0, 0),
                EntityBlockingShieldBehavior.getInstance()
        ));
        shields.add(new Shield(
                world,
                BoundingBox.of(finalLocation.clone().add(-SHIELD_RADIUS, SHIELD_RADIUS - 0.005, -SHIELD_RADIUS), finalLocation.clone().add(SHIELD_RADIUS, SHIELD_RADIUS + 0.005, SHIELD_RADIUS)),
                new Vector(0, 1, 0),
                EntityBlockingShieldBehavior.getInstance()
        ));

        ShieldManager.getInstance().getShields().addAll(shields);

        new AbilityRunnable() {
            BossBar bossbar;

            @Override
            protected synchronized void start() {
                bossbar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.getName(), BarColor.PURPLE, BarStyle.SOLID);
                bossbar.addPlayer(player);
            }

            @Override
            protected synchronized void tick() {
                if (!abilityActive.get()) {
                    cancel();
                }
                bossbar.setProgress(count / (double) info.getDuration() * 10);
            }

            @Override
            protected synchronized void end() {
                bossbar.removeAll();
                ShieldManager.getInstance().getShields().removeAll(shields);
                abilityActive.set(false);
                cooldown.setCooldown(info.getCooldown());
            }
        }.runTaskRepeated(this, 0, 10, info.getDuration() / 10);
    }
}
