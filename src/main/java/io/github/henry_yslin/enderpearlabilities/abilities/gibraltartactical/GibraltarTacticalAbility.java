package io.github.henry_yslin.enderpearlabilities.abilities.gibraltartactical;

import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henry_yslin.enderpearlabilities.managers.shield.EntityBlockingShieldBehavior;
import io.github.henry_yslin.enderpearlabilities.managers.shield.Shield;
import io.github.henry_yslin.enderpearlabilities.managers.shield.ShieldManager;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
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

public class GibraltarTacticalAbility extends Ability {

    static final int PROJECTILE_LIFETIME = 60;
    static final double PROJECTILE_SPEED = 1;
    static final boolean PROJECTILE_GRAVITY = true;
    static final double SHIELD_RADIUS = 6;

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        super.setConfigDefaults(config);
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 600);
        config.addDefault("cooldown", 1500);
    }

    public GibraltarTacticalAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("gibraltar-tactical")
                .name("Dome of Protection")
                .origin("Apex - Gibraltar")
                .description("Blocks incoming and outgoing attacks.")
                .usage("Right click to throw a disc that projects a shield around it.")
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
    final AtomicBoolean blockShoot = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);

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
        cooldown.startCooldown(info.cooldown);
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;
        if (chargingUp.get()) return;
        if (abilityActive.get()) return;

        new FunctionChain(
                next -> {
                    PlayerUtils.consumeEnderPearl(player);
                    next.run();
                },
                next -> AbilityUtils.chargeUpSequence(this, player, info.chargeUp, chargingUp, next),
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
                bossbar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.name, BarColor.PURPLE, BarStyle.SOLID);
                bossbar.addPlayer(player);
            }

            @Override
            protected synchronized void tick() {
                if (!abilityActive.get()) {
                    cancel();
                }
                bossbar.setProgress(count / (double) info.duration * 10);
            }

            @Override
            protected synchronized void end() {
                bossbar.removeAll();
                ShieldManager.getInstance().getShields().removeAll(shields);
                abilityActive.set(false);
                cooldown.startCooldown(info.cooldown);
            }
        }.runTaskRepeated(this, 0, 10, info.duration / 10);
    }
}
