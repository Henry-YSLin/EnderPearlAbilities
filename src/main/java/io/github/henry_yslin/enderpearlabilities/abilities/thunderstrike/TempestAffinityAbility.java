package io.github.henry_yslin.enderpearlabilities.abilities.thunderstrike;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityCooldown;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.SingleUseCooldown;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TempestAffinityAbility extends Ability<TempestAffinityAbilityInfo> {

    static final int STRIKE_EXTEND_DURATION = 5 * 20;
    static final int STRIKE_CREDIT_MAX_DISTANCE = 128;

    public TempestAffinityAbility(Plugin plugin, TempestAffinityAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicInteger abilityDuration = new AtomicInteger(0);
    RainstormRunnable rainstormRunnable;
    Player owner;

    @Override
    protected AbilityCooldown createCooldown() {
        return new SingleUseCooldown(this, player);
    }

    @Override
    public boolean isActive() {
        return abilityDuration.get() > 0;
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
        owner = player;
        chargingUp.set(false);
        abilityDuration.set(0);
        cooldown.setCooldown(info.getCooldown());


        if (rainstormRunnable != null && !rainstormRunnable.isCancelled())
            rainstormRunnable.cancel();
        rainstormRunnable = new RainstormRunnable(player);
        rainstormRunnable.runTaskTimer(this, 0, 10);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.LIGHTNING) return;
        if (!event.getDamager().getWorld().equals(owner.getWorld())) return;
        if (event.getDamager().getLocation().distanceSquared(owner.getLocation()) > STRIKE_CREDIT_MAX_DISTANCE * STRIKE_CREDIT_MAX_DISTANCE)
            return;
        extendDuration(livingEntity);
    }

    @EventHandler
    public void onLightningStrike(LightningStrikeEvent event) {
        if (!event.getWorld().equals(owner.getWorld())) return;
        if (event.getLightning().getLocation().distanceSquared(owner.getLocation()) > STRIKE_CREDIT_MAX_DISTANCE * STRIKE_CREDIT_MAX_DISTANCE)
            return;
        for (Entity entity : event.getLightning().getNearbyEntities(2, 2, 2)) {
            if (entity instanceof LivingEntity livingEntity) {
                if (entity.isDead()) {
                    extendDuration(livingEntity);
                }
            }
        }
    }

    private void extendDuration(LivingEntity livingEntity) {
        int duration = abilityDuration.get();
        if (duration <= 0) return;
        abilityDuration.set(Math.min(info.getDuration(), duration + STRIKE_EXTEND_DURATION));
        livingEntity.getWorld().spawnParticle(Particle.DRAGON_BREATH, livingEntity.getEyeLocation(), 20, 1, 1, 1, 0.1);
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;

        event.setCancelled(true);

        if (!cooldown.isAbilityUsable()) return;
        if (chargingUp.get()) return;
        if (abilityDuration.get() > 0) return;
        if (player.getWorld().getEnvironment() == World.Environment.NETHER || player.getWorld().getEnvironment() == World.Environment.THE_END) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "There is no thunderstorm here"));
            return;
        }

        AbilityUtils.consumeEnderPearl(this, player);
        EnderPearlAbilities.getInstance().emitEvent(
                EventListener.class,
                new AbilityActivateEvent(this),
                EventListener::onAbilityActivate
        );

        new FunctionChain(
                next -> AbilityUtils.chargeUpSequence(this, player, info.getChargeUp(), chargingUp, next),
                next -> new AbilityRunnable() {
                    BossBar bossBar;
                    World world;

                    @Override
                    protected void start() {
                        abilityDuration.set(info.getDuration());
                        bossBar = Bukkit.createBossBar(ChatColor.RED + info.getName(), BarColor.RED, BarStyle.SOLID);
                        bossBar.addPlayer(player);

                        world = player.getWorld();

                        for (Player playerInWorld : world.getPlayers()) {
                            world.playSound(playerInWorld, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 0.5f);
                        }

                        world.spawnParticle(Particle.ELECTRIC_SPARK, player.getEyeLocation(), 20, 0.5, 0.5, 0.5, 0.02);
                    }

                    @Override
                    protected void tick() {
                        int duration = abilityDuration.get();
                        if (!player.isValid() || duration <= 0) {
                            cancel();
                            return;
                        }
                        if (player.getWorld().getEnvironment() == World.Environment.NETHER || player.getWorld().getEnvironment() == World.Environment.THE_END) {
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "There is no thunderstorm here"));
                            cancel();
                            return;
                        }
                        bossBar.setProgress((double) duration / info.getDuration());

                        world.setStorm(true);
                        world.setThundering(true);

                        abilityDuration.set(duration - 10);
                        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0);
                    }

                    @Override
                    protected void end() {
                        abilityDuration.set(0);
                        bossBar.removeAll();

                        world.setStorm(false);
                        world.setThundering(false);

                        cooldown.setCooldown(info.getCooldown());
                    }
                }.runTaskTimer(this, 0, 10)
        ).execute();
    }
}
