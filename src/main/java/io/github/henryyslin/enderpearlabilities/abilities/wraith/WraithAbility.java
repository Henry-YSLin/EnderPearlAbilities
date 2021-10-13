package io.github.henryyslin.enderpearlabilities.abilities.wraith;

import io.github.henryyslin.enderpearlabilities.abilities.Ability;
import io.github.henryyslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henryyslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henryyslin.enderpearlabilities.utils.AbilityRunnable;
import io.github.henryyslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henryyslin.enderpearlabilities.utils.FunctionChain;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class WraithAbility extends Ability {

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 20);
        config.addDefault("duration", 140);
        config.addDefault("cooldown", 160);
    }

    public WraithAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("wraith")
                .name("Into The Void")
                .origin("Apex - Wraith")
                .description("For a short duration, improve vision and switch to spectator mode for fast flying, leaving a particle trail behind. Cannot go through walls.\nPassive ability: run faster when you sprint, jump higher when you sneak.")
                .usage("Right click to activate the ability. Teleporting with the spectator menu is not allowed when the ability is active. Triple click sneak to exit early.")
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
        if (player.getGameMode() == GameMode.SPECTATOR) {
            Block headBlock = player.getWorld().getBlockAt(player.getEyeLocation());
            if (!headBlock.getType().isOccluding()) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
        abilityActive.set(false);
        cooldown.startCooldown(info.cooldown);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (player != null) {
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.JUMP);
        }
    }

    int sneakCount = 0;

    @EventHandler
    public synchronized void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;

        if (event.isSneaking()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 100000, 1, true, true));
        } else {
            player.removePotionEffect(PotionEffectType.JUMP);
        }

        if (!abilityActive.get()) return;

        int lastSneakCount = sneakCount;
        sneakCount++;
        new AbilityRunnable() {
            @Override
            public void tick() {
                if (sneakCount - lastSneakCount >= 6) {
                    abilityActive.set(false);
                }
            }
        }.runTaskLater(this, 10);
    }

    @EventHandler
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        if (event.isSprinting())
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100000, 1, true, true));
        else
            player.removePotionEffect(PotionEffectType.SPEED);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) return;
        if (!abilityActive.get()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;

        AtomicReference<GameMode> prevGameMode = new AtomicReference<>(null);

        new FunctionChain(
                next -> {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 1, 0);
                    AbilityUtils.consumeEnderPearl(player);
                    next.run();
                },
                next -> AbilityUtils.chargeUpSequence(this, player, info.chargeUp, next),
                next -> {
                    abilityActive.set(true);
                    prevGameMode.set(player.getGameMode());
                    player.setGameMode(GameMode.SPECTATOR);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 100, 1));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 100, 1));
                    player.teleport(player.getLocation().add(0, 0.5, 0));
                    next.run();
                },
                next -> new AbilityRunnable() {
                    BossBar bossbar;
                    Location lastLocation;

                    @Override
                    protected synchronized void start() {
                        player.setCooldown(Material.ENDER_PEARL, info.duration);
                        bossbar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.name, BarColor.PURPLE, BarStyle.SOLID);
                        bossbar.addPlayer(player);
                        lastLocation = player.getLocation();
                    }

                    @Override
                    protected synchronized void tick() {
                        if (!abilityActive.get()) {
                            cancel();
                        }
                        bossbar.setProgress(count / (double) info.duration);
                        Block headBlock = player.getWorld().getBlockAt(player.getEyeLocation());
                        if (headBlock.getType().isOccluding()) {
                            player.teleport(lastLocation);
                        }
                        lastLocation = player.getLocation();
                        player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation(), 10, 0.5, 1, 0.5, 0.02);
                    }

                    @Override
                    protected synchronized void end() {
                        bossbar.removeAll();
                        if (!abilityActive.get()) {
                            player.teleport(player.getLocation().add(0, 1, 0));
                        }
                        Block headBlock = player.getWorld().getBlockAt(player.getEyeLocation());
                        if (headBlock.getType().isOccluding()) {
                            player.teleport(lastLocation);
                        }
                        next.run();
                    }
                }.runTaskRepeated(this, 0, 1, info.duration),
                next -> {
                    player.setGameMode(prevGameMode.get());
                    abilityActive.set(false);
                    sneakCount = 0;
                    player.removePotionEffect(PotionEffectType.CONDUIT_POWER);
                    player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                    cooldown.startCooldown(info.cooldown);
                    next.run();
                }
        ).execute();
    }
}
