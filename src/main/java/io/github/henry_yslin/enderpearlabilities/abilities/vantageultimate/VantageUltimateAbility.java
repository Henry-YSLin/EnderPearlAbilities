package io.github.henry_yslin.enderpearlabilities.abilities.vantageultimate;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.*;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henry_yslin.enderpearlabilities.utils.FunctionChain;
import io.github.henry_yslin.enderpearlabilities.utils.ProjectileUtils;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class VantageUltimateAbility extends Ability<VantageUltimateAbilityInfo> {

    static final int PROJECTILE_LIFETIME = 60;

    public VantageUltimateAbility(Plugin plugin, VantageUltimateAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    AbilityRunnable sniperRunnable;
    final AtomicReference<ItemStack> sniper = new AtomicReference<>();

    @Override
    protected AbilityCooldown createCooldown() {
        return new MultipleChargeCooldown(this, player, info.getCharge());
    }

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
        chargingUp.set(false);
        abilityActive.set(false);
        cooldown.setCooldown(info.getCooldown());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        if (sniperRunnable != null && !sniperRunnable.isCancelled())
            sniperRunnable.cancel();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!event.getEntity().equals(player)) return;
        if (sniperRunnable != null && !sniperRunnable.isCancelled())
            sniperRunnable.cancel();
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!event.getPlayer().equals(player)) return;
        if (!event.getItemDrop().getItemStack().equals(sniper.get())) return;
        event.setCancelled(true);
        AbilityUtils.delay(this, 1, () -> {
            if (sniperRunnable != null && !sniperRunnable.isCancelled())
                sniperRunnable.cancel();
        }, true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getWhoClicked().equals(player)) return;
        if (!Objects.equals(event.getCurrentItem(), sniper.get())) return;
        event.setCancelled(true);
        AbilityUtils.delay(this, 1, () -> {
            ((Player) event.getWhoClicked()).updateInventory();
        }, true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!event.getWhoClicked().equals(player)) return;
        if (!Objects.equals(event.getCursor(), sniper.get()) && !Objects.equals(event.getOldCursor(), sniper.get()))
            return;
        event.setCancelled(true);
        AbilityUtils.delay(this, 1, () -> {
            ((Player) event.getWhoClicked()).updateInventory();
        }, true);
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!event.getEntity().equals(player)) return;
        if (!Objects.equals(event.getBow(), sniper.get())) return;
        if (!abilityActive.get()) return;

        event.getProjectile().setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.getCodeName(), ownerName)));
        event.getProjectile().setGravity(false);
        event.getProjectile().setVelocity(event.getProjectile().getVelocity().multiply(1.5));
        cooldown.setCooldown(info.getCooldown());
        AbilityUtils.delay(this, PROJECTILE_LIFETIME, () -> {
            if (event.getProjectile().isValid())
                event.getProjectile().remove();
        }, true);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!AbilityUtils.verifyAbilityCouple(this, event.getEntity())) return;
        if (event.getHitEntity() == null) return;
        if (!(event.getHitEntity() instanceof LivingEntity livingEntity)) return;

        Location hitLocation = ProjectileUtils.correctProjectileHitLocation(event.getEntity(), entity -> !entity.equals(event.getEntity().getShooter()));
        livingEntity.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, hitLocation, 1, 0, 0, 0, 0, null, true);
        boolean headshot = livingEntity.getEyeLocation().distanceSquared(hitLocation) < 0.6 * 0.6;
        if (headshot) {
            livingEntity.addPotionEffect(PotionEffectType.WITHER.createEffect(5 * 20, 1));
            livingEntity.getWorld().spawnParticle(Particle.SOUL, livingEntity.getEyeLocation(), 20, 0.5, 0.5, 0.5, 0.05, null, true);
        }
        new AbilityRunnable() {
            BossBar bossBar;

            @Override
            protected void start() {
                livingEntity.setMetadata("marked", new FixedMetadataValue(plugin, this));
                if (livingEntity instanceof Player p) {
                    bossBar = Bukkit.createBossBar(ChatColor.RED + "Marked", BarColor.RED, BarStyle.SOLID);
                    bossBar.addPlayer(p);
                }
            }

            @Override
            protected void tick() {
                if (bossBar != null) {
                    bossBar.setProgress(count * 5d / info.getDuration());
                }
                if (!livingEntity.isValid()) {
                    cancel();
                    return;
                }
                livingEntity.addPotionEffect(PotionEffectType.GLOWING.createEffect(10, 0));
            }

            @Override
            protected void end() {
                if (bossBar != null) {
                    bossBar.removeAll();
                }
                livingEntity.removeMetadata("marked", plugin);
            }
        }.runTaskRepeated(this, 0, 5, info.getDuration() / 5);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        if (!livingEntity.hasMetadata("marked")) return;

        event.setDamage(event.getDamage() * 2);
        livingEntity.getWorld().spawnParticle(Particle.HEART, livingEntity.getEyeLocation(), 5, 0.5, 0.5, 0.5, 0.1);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.getName().equals(ownerName)) return;
        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;

        event.setCancelled(true);

        if (chargingUp.get()) return;
        if (abilityActive.get()) return;
        if (!cooldown.isAbilityUsable()) return;

        new FunctionChain(
                next -> AbilityUtils.chargeUpSequence(this, player, info.getChargeUp(), chargingUp, next),
                next -> {
                    PlayerInventory inventory = player.getInventory();
                    int freeSlots = 0;
                    for (int i = 0; i <= 35; i++) {
                        if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                            freeSlots++;
                        }
                    }
                    if (!inventory.contains(Material.ARROW) && !inventory.contains(Material.SPECTRAL_ARROW) && !inventory.contains(Material.TIPPED_ARROW))
                        freeSlots -= 1;
                    if (inventory.getItemInOffHand().getType() == Material.AIR)
                        freeSlots += 1;
                    if (freeSlots < 1) {
                        player.sendTitle("", ChatColor.RED + "Not enough inventory space!", 0, 30, 10);
                        return;
                    }
                    next.run();
                },
                next -> {
                    if (sniperRunnable != null && !sniperRunnable.isCancelled())
                        sniperRunnable.cancel();

                    Ability<?> self = this;
                    (sniperRunnable = new AbilityRunnable() {
                        BossBar bossBar;

                        @Override
                        protected void start() {
                            bossBar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.getName(), BarColor.PURPLE, BarStyle.SOLID);
                            bossBar.addPlayer(player);
                            abilityActive.set(true);

                            AbilityUtils.consumeEnderPearl(self, player);
                            EnderPearlAbilities.getInstance().emitEvent(
                                    EventListener.class,
                                    new AbilityActivateEvent(self),
                                    EventListener::onAbilityActivate
                            );

                            if (player.getInventory().getItemInOffHand().getType() != Material.AIR) {
                                player.getInventory().addItem(player.getInventory().getItemInOffHand());
                            }
                            ItemStack bow = new ItemStack(Material.BOW, 1);
                            ItemMeta meta = Bukkit.getServer().getItemFactory().getItemMeta(Material.BOW);
                            if (meta != null) {
                                meta.setUnbreakable(true);
                                meta.setDisplayName(ChatColor.LIGHT_PURPLE + info.getName());
                                meta.setLocalizedName(ChatColor.LIGHT_PURPLE + info.getName());
                                meta.setLore(List.of(ChatColor.WHITE + "Highly accurate sniper"));
                            }
                            bow.setItemMeta(meta);
                            bow.addEnchantment(Enchantment.VANISHING_CURSE, 1);
                            bow.addEnchantment(Enchantment.ARROW_INFINITE, 1);
                            player.getInventory().setItemInOffHand(bow);
                            player.getInventory().addItem(new ItemStack(Material.ARROW, 1));
                            sniper.set(bow);
                        }

                        @Override
                        protected void tick() {
                            if (!player.isValid()) {
                                cancel();
                                return;
                            }
                            if (!cooldown.isAbilityUsable()) {
                                cancel();
                                return;
                            }
                            bossBar.setProgress(((MultipleChargeCooldown) cooldown).getAvailableCharge() / (double) info.getCharge());
                        }

                        @Override
                        protected void end() {
                            bossBar.removeAll();
                            abilityActive.set(false);
                            if (sniper.get() != null) {
                                for (int i = 0; i <= 40; i++) {
                                    if (Objects.equals(player.getInventory().getItem(i), sniper.get())) {
                                        player.getInventory().setItem(i, null);
                                    }
                                }
                                player.updateInventory();
                                sniper.set(null);
                            }
                        }
                    }).runTaskTimer(this, 0, 1);
                }
        ).execute();
    }
}
