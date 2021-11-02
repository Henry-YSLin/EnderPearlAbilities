package io.github.henryyslin.enderpearlabilities.abilities.wraith;

import io.github.henryyslin.enderpearlabilities.abilities.Ability;
import io.github.henryyslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henryyslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henryyslin.enderpearlabilities.utils.AbilityRunnable;
import io.github.henryyslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henryyslin.enderpearlabilities.utils.FunctionChain;
import io.github.henryyslin.enderpearlabilities.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.atomic.AtomicBoolean;

public class WraithAbility extends Ability {
    static final int CANCEL_DELAY = 10;

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 20);
        config.addDefault("duration", 200);
        config.addDefault("cooldown", 300);
    }

    public WraithAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("wraith")
                .name("Into The Void")
                .origin("Apex - Wraith")
                .description("Reposition quickly through the safety of void space, avoiding all damage and interactions.\nPassive ability: You have faster sprint speed.")
                .usage("Right click with an ender pearl to activate the ability. Right click again to exit early. You may not interact with anything while the ability is active.")
                .activation(ActivationHand.MainHand);

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
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!player.getName().equals(ownerName)) return;
        if (!abilityActive.get()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.getName().equals(ownerName)) return;
        if (!abilityActive.get()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerPickupArrow(PlayerPickupArrowEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        if (!abilityActive.get()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        if (!abilityActive.get()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        if (abilityActive.get()) return;
        if (event.isSprinting())
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1000000, 0, true, true));
        else
            player.removePotionEffect(PotionEffectType.SPEED);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) {
            if (player.getName().equals(ownerName) && abilityActive.get()) event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        if (player.getName().equals(ownerName) && abilityActive.get()) {
            cancelAbility.set(true);
            return;
        }

        if (cooldown.getCoolingDown()) return;
        if (chargingUp.get()) return;

        new FunctionChain(
                next -> {
                    PlayerUtils.consumeEnderPearl(player);
                    next.run();
                },
                next -> AbilityUtils.chargeUpSequence(this, player, info.chargeUp, chargingUp, next),
                next -> {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 1, 0);
                    cancelAbility.set(false);
                    abilityActive.set(true);
                    player.setCollidable(false);
                    player.setInvulnerable(true);
                    player.setInvisible(true);
                    for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                        onlinePlayer.hidePlayer(plugin, player);
                    }
                    player.addPotionEffect(PotionEffectType.CONDUIT_POWER.createEffect(info.duration, 1));
                    player.addPotionEffect(PotionEffectType.NIGHT_VISION.createEffect(info.duration, 1));
                    player.addPotionEffect(PotionEffectType.SPEED.createEffect(info.duration, 1));
                    next.run();
                },
                next -> new AbilityRunnable() {
                    BossBar bossbar;
                    int cancelTick = CANCEL_DELAY;

                    @Override
                    protected synchronized void start() {
                        bossbar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.name, BarColor.PURPLE, BarStyle.SOLID);
                        bossbar.addPlayer(player);
                    }

                    @Override
                    protected synchronized void tick() {
                        if (!abilityActive.get() || !player.isValid()) {
                            cancel();
                        }
                        if (cancelAbility.get()) {
                            cancelTick--;
                            player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 5, 0.5, 1, 0.5, 0.02, null, true);
                            if (cancelTick <= 0) abilityActive.set(false);
                        }
                        bossbar.setProgress(count / (double) info.duration);
                        player.setRemainingAir(player.getMaximumAir());
                        player.setFireTicks(0);
                        if (count < CANCEL_DELAY)
                            player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 5, 0.5, 1, 0.5, 0.02, null, true);
                        player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation(), 10, 0.5, 1, 0.5, 0.02, null, true);
                    }

                    @Override
                    protected synchronized void end() {
                        bossbar.removeAll();
                        next.run();
                    }
                }.runTaskRepeated(this, 0, 1, info.duration),
                next -> {
                    for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                        onlinePlayer.showPlayer(plugin, player);
                    }
                    abilityActive.set(false);
                    cancelAbility.set(false);
                    player.setCollidable(true);
                    player.setInvulnerable(false);
                    player.setInvisible(false);
                    player.setFireTicks(0);
                    player.removePotionEffect(PotionEffectType.CONDUIT_POWER);
                    player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                    player.removePotionEffect(PotionEffectType.SPEED);
                    if (player.isSprinting())
                        player.addPotionEffect(PotionEffectType.SPEED.createEffect(1000000, 0));
                    cooldown.startCooldown(info.cooldown);
                    next.run();
                }
        ).execute();
    }
}
