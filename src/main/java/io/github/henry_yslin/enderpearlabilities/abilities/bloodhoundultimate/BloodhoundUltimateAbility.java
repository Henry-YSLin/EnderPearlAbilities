package io.github.henry_yslin.enderpearlabilities.abilities.bloodhoundultimate;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityCooldown;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.SingleUseCooldown;
import io.github.henry_yslin.enderpearlabilities.abilities.bloodhoundtactical.BloodhoundTacticalAbility;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BloodhoundUltimateAbility extends Ability<BloodhoundUltimateAbilityInfo> {

    static final int KILL_EXTEND_DURATION = 15 * 20;
    static final int TACTICAL_COOLDOWN = 3 * 20;

    public BloodhoundUltimateAbility(Plugin plugin, BloodhoundUltimateAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicInteger abilityDuration = new AtomicInteger(0);

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
        chargingUp.set(false);
        abilityDuration.set(0);
        cooldown.setCooldown(info.getCooldown());
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        if (livingEntity.getHealth() - event.getFinalDamage() > 0) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!player.getName().equals(ownerName)) return;
        int duration = abilityDuration.get();
        if (duration <= 0) return;
        abilityDuration.set(Math.min(info.getDuration(), duration + KILL_EXTEND_DURATION));
        livingEntity.getWorld().spawnParticle(Particle.CRIT_MAGIC, livingEntity.getEyeLocation(), 20, 0.5, 0.5, 0.5, 0.1);
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;

        event.setCancelled(true);

        if (!cooldown.isAbilityUsable()) return;
        if (chargingUp.get()) return;
        if (abilityDuration.get() > 0) return;

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
                    Ability<?> tacticalAbility;

                    @Override
                    protected void start() {
                        abilityDuration.set(info.getDuration());
                        bossBar = Bukkit.createBossBar(ChatColor.RED + info.getName(), BarColor.RED, BarStyle.SOLID);
                        bossBar.addPlayer(player);
                        tacticalAbility = EnderPearlAbilities.getInstance().getAbilities().stream()
                                .filter(ability -> ability instanceof BloodhoundTacticalAbility && ability.getOwnerName().equals(ownerName))
                                .findFirst()
                                .orElse(null);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.3f, 2);
                        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getEyeLocation(), 20, 0.5, 0.5, 0.5, 0.02);
                        player.addPotionEffect(PotionEffectType.NIGHT_VISION.createEffect(15, 0));
                        player.addPotionEffect(PotionEffectType.CONDUIT_POWER.createEffect(15, 0));
                        player.addPotionEffect(PotionEffectType.SPEED.createEffect(15, 1));
                    }

                    @Override
                    protected void tick() {
                        int duration = abilityDuration.get();
                        if (!player.isValid() || duration <= 0) {
                            cancel();
                            return;
                        }
                        bossBar.setProgress((double) duration / info.getDuration());
                        abilityDuration.set(duration - 10);
                        if (tacticalAbility != null) {
                            if (tacticalAbility.getCooldown().getCooldownTicks() > TACTICAL_COOLDOWN)
                                tacticalAbility.getCooldown().setCooldown(TACTICAL_COOLDOWN);
                        }
                        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0);
                        player.addPotionEffect(PotionEffectType.NIGHT_VISION.createEffect(15, 0));
                        player.addPotionEffect(PotionEffectType.CONDUIT_POWER.createEffect(15, 0));
                        player.addPotionEffect(PotionEffectType.SPEED.createEffect(15, 1));
                    }

                    @Override
                    protected void end() {
                        abilityDuration.set(0);
                        bossBar.removeAll();
                        cooldown.setCooldown(info.getCooldown());
                    }
                }.runTaskTimer(this, 0, 10)
        ).execute();
    }
}
