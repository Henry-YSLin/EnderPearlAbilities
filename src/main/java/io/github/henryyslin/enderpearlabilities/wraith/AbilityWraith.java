package io.github.henryyslin.enderpearlabilities.wraith;

import io.github.henryyslin.enderpearlabilities.Ability;
import io.github.henryyslin.enderpearlabilities.AbilityCooldown;
import io.github.henryyslin.enderpearlabilities.ActivationHand;
import io.github.henryyslin.enderpearlabilities.utils.AdvancedRunnable;
import io.github.henryyslin.enderpearlabilities.utils.FunctionChain;
import io.github.henryyslin.enderpearlabilities.utils.NextFunction;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AbilityWraith implements Ability {
    public String getName() { return "Into The Void"; }
    public String getOrigin() { return "Apex - Wraith"; }
    public String getConfigName() { return "wraith"; }
    public String getDescription() { return "For a short duration, improve vision and switch to spectator mode for fast flying, leaving a particle trail behind. Cannot go through walls."; }
    public ActivationHand getActivation() { return ActivationHand.MainHand; }
    public int getChargeUp() { return 20; }
    public int getDuration() { return 100; }
    public int getCooldown() { return 150; }

    final Plugin plugin;
    final FileConfiguration config;
    final String ownerName;
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    AbilityCooldown cooldown;

    public AbilityWraith(Plugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
        this.ownerName = config.getString(getConfigName());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getName().equals(ownerName)) {
            Block headBlock = player.getWorld().getBlockAt(player.getEyeLocation());
            if (!headBlock.getType().isOccluding()) {
                player.setGameMode(GameMode.SURVIVAL);
            }
            cooldown = new AbilityCooldown(plugin, player);
            cooldown.StartCooldown(getCooldown());
        }
    }

    int sneakCount = 0;

    @EventHandler
    public synchronized void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        if (!abilityActive.get()) return;

        int lastSneakCount = sneakCount;
        sneakCount++;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (sneakCount - lastSneakCount >= 6) {
                    abilityActive.set(false);
                }
            }
        }.runTaskLater(plugin, 10);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) return;
        if (!abilityActive.get()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerClicks(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack item = event.getItem();

        if (!player.getName().equals(ownerName)) return;
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        if (item == null || item.getType() != Material.ENDER_PEARL) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;
        
        AtomicReference<GameMode> prevGameMode = new AtomicReference<>(null);

        new FunctionChain(
            (NextFunction next) -> {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 1, 0);
                if (player.getGameMode() != GameMode.CREATIVE) {
                    player.getInventory().removeItem(new ItemStack(Material.ENDER_PEARL, 1));
                }
                next.Invoke();
            },
            (NextFunction next) -> {
                new AdvancedRunnable() {
                    BossBar bossbar;

                    @Override
                    protected synchronized void start() {
                        player.setCooldown(Material.ENDER_PEARL, getChargeUp());
                        bossbar = Bukkit.createBossBar("Charging up", BarColor.WHITE, BarStyle.SOLID);
                        bossbar.addPlayer(player);
                    }

                    @Override
                    protected synchronized void tick() {
                        bossbar.setProgress(count / (double)getChargeUp());
                        player.getWorld().spawnParticle(Particle.WHITE_ASH, player.getLocation(), 5, 0.5, 0.5, 0.5, 0.02);
                    }

                    @Override
                    protected synchronized void end() {
                        bossbar.removeAll();
                        next.Invoke();
                    }
                }.runTaskRepeated(plugin, 0, 1, getChargeUp());
            },
            (NextFunction next) -> {
                abilityActive.set(true);
                prevGameMode.set(player.getGameMode());
                player.setGameMode(GameMode.SPECTATOR);
                player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 100, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 100, 1));
                player.teleport(player.getLocation().add(0, 0.5, 0));
                next.Invoke();
            },
            (NextFunction next) -> {
                new AdvancedRunnable() {
                    BossBar bossbar;
                    Location lastLocation;

                    @Override
                    protected synchronized void start() {
                        player.setCooldown(Material.ENDER_PEARL, getChargeUp());
                        bossbar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + getName(), BarColor.PURPLE, BarStyle.SOLID);
                        bossbar.addPlayer(player);
                        lastLocation = player.getLocation();
                    }

                    @Override
                    protected synchronized void tick() {
                        if (!abilityActive.get()) {
                            cancel();
                        }
                        bossbar.setProgress(count / (double)getDuration());
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
                        next.Invoke();
                    }
                }.runTaskRepeated(plugin, 0, 1, getDuration());
            },
            (NextFunction next) -> {
                player.setGameMode(prevGameMode.get());
                abilityActive.set(false);
                sneakCount = 0;
                player.removePotionEffect(PotionEffectType.CONDUIT_POWER);
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                cooldown.StartCooldown(getCooldown());
                next.Invoke();
            }
        ).execute();
    }
}
