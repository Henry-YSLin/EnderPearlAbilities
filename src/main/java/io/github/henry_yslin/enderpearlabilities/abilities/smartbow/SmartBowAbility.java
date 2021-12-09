package io.github.henry_yslin.enderpearlabilities.abilities.smartbow;

import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henry_yslin.enderpearlabilities.abilities.wraithtactical.VoicesFromTheVoidListener;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.concurrent.atomic.AtomicBoolean;

public class SmartBowAbility extends Ability {

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        super.setConfigDefaults(config);
        config.addDefault("charge-up", 20);
        config.addDefault("duration", 140);
        config.addDefault("cooldown", 500);
    }

    public SmartBowAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("smart-bow")
                .name("Smart Bow")
                .origin("Titanfall - Smart Pistol")
                .description("Bow with built-in aimbot.")
                .usage("Right click.")
                .activation(ActivationHand.OffHand);

        if (config != null)
            builder
                    .chargeUp(config.getInt("charge-up"))
                    .duration(config.getInt("duration"))
                    .cooldown(config.getInt("cooldown"));

        info = builder.build();

        subListeners.add(new VoicesFromTheVoidListener(plugin, this, config));
    }

    @Override
    public AbilityInfo getInfo() {
        return info;
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    final AtomicBoolean cancelAbility = new AtomicBoolean(false);

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
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        event.getPlayer().sendMessage(event.toString());
        event.getPlayer().sendMessage(event.getAction().toString());
        event.getPlayer().sendMessage("use interacted block:" + event.useInteractedBlock());
        event.getPlayer().sendMessage("item:" + (event.getItem() == null ? "null" : event.getItem().toString()));
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.getName().equals(ownerName)) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;

        // https://minecraft.fandom.com/wiki/Entity#Motion_of_entities
        Vector velocity = arrow.getVelocity();
        Location location = arrow.getLocation();

        for (int i = 0; i < 100; i++) {
            location.add(velocity);
            velocity.multiply(0.99);
            velocity.add(new Vector(0, -1 / 20f, 0));
            arrow.getWorld().spawnParticle(Particle.END_ROD, location, 1, 0, 0, 0, 0, null, true);
        }

        velocity = arrow.getVelocity();
        location = arrow.getLocation();

        for (int i = 0; i < 100; i++) {
            location.add(velocity);
            velocity.add(new Vector(0, -1 / 20f, 0));
            arrow.getWorld().spawnParticle(Particle.END_ROD, location, 1, 0, 0, 0, 0, null, true);
        }

        (new AbilityRunnable() {
            @Override
            protected void tick() {
                super.tick();
                if (arrow.isOnGround() || !arrow.isValid()) {
                    cancel();
                    return;
                }
                arrow.getWorld().spawnParticle(Particle.END_ROD, arrow.getLocation(), 1, 0, 0, 0, 0, null, true);
            }
        }).runTaskTimer(this, 0, 1);
    }
}
