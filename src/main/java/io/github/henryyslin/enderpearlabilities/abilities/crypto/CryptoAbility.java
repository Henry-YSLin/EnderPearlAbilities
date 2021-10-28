package io.github.henryyslin.enderpearlabilities.abilities.crypto;

import io.github.henryyslin.enderpearlabilities.abilities.Ability;
import io.github.henryyslin.enderpearlabilities.abilities.AbilityCouple;
import io.github.henryyslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henryyslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henryyslin.enderpearlabilities.utils.*;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CryptoAbility extends Ability {

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 20);
        config.addDefault("duration", 0);
        config.addDefault("cooldown", 800);
    }

    public CryptoAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("crypto")
                .name("Surveillance Drone")
                .origin("Apex - Crypto")
                .description("Deploys an aerial drone. 40-second cooldown if destroyed.")
                .usage("Right click to deploy the drone.")
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
    final AtomicReference<LivingEntity> drone = new AtomicReference<>();
    final AtomicReference<NPC> dummy = new AtomicReference<>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        super.onPlayerJoin(event);
        cleanUp();
        Player player = event.getPlayer();
        if (player.getName().equals(ownerName)) {
            setUpPlayer(player);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        cleanUp();
        if (player != null) {
            setUpPlayer(player);
        }
    }

    private void removeAllNPCs() {
        CitizensAPI.getNPCRegistries().forEach(registry -> {
            List<NPC> toBeRemoved = new ArrayList<>();
            registry.forEach(npc -> {
                if (npc.isSpawned()) {
                    if (!npc.getEntity().hasMetadata("ability")) return;
                    if (!AbilityUtils.verifyAbilityCouple(this, npc.getEntity())) return;
                }
                toBeRemoved.add(npc);
            });
            toBeRemoved.forEach(registry::deregister);
        });
    }

    private void cleanUp() {
        removeAllNPCs();
        LivingEntity d = drone.get();
        if (d != null && d.isValid()) {
            d.remove();
            drone.set(null);
        }
        NPC n = dummy.get();
        if (n != null) {
            if (player != null) {
                player.setGameMode(GameMode.SURVIVAL);
                player.teleport(n.getStoredLocation());
            }
            n.getOwningRegistry().deregister(n);
        }
    }

    private void setUpPlayer(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }

        abilityActive.set(false);
        cooldown.startCooldown(info.cooldown);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (player != null) {
            if (dummy.get() != null) {
                player.teleport(dummy.get().getStoredLocation());
            }
            if (player.getGameMode() == GameMode.SPECTATOR) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
        cleanUp();
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) return;
        if (!abilityActive.get()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDropItem(EntityDropItemEvent event) {
        if (!AbilityUtils.verifyAbilityCouple(this, event.getEntity())) return;
        event.getItemDrop().remove();
    }

    private void warnNotEnoughSpace(Player player) {
        player.sendTitle(" ", ChatColor.LIGHT_PURPLE + "Not enough space", 5, 20, 10);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().equals(drone.get())) {
            // if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) event.setCancelled(true);
        } else {
            NPC npc = dummy.get();
            if (npc != null && event.getEntity().equals(npc.getEntity())) {
                player.damage(event.getDamage());
                player.setLastDamageCause(event);
                LivingEntity livingNpc = (LivingEntity) npc.getEntity();
                player.setHealth(Math.ceil(livingNpc.getHealth()));
                player.sendMessage(player.getHealth() + "");
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!event.getEntity().equals(player)) return;
        if (!abilityActive.get()) return;
        cleanUp();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().equals(drone.get())) {
            // TODO
        } else {
            NPC npc = dummy.get();
            if (npc != null && !npc.isSpawned() && AbilityUtils.verifyAbilityCouple(this, event.getEntity())) {
                player.setHealth(event.getEntity().getHealth());
                player.sendMessage(player.getHealth() + "");
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;
        if (abilityActive.get()) return;

        World world = player.getWorld();

        // Check if there's enough space to deploy drone
        Location eyeLocation = player.getEyeLocation();
        Location deployLocation = eyeLocation.clone().add(eyeLocation.getDirection().multiply(2));
        if (deployLocation.getBlock().getType() != Material.AIR) {
            warnNotEnoughSpace(player);
            return;
        }
        RayTraceResult result = world.rayTraceBlocks(eyeLocation, eyeLocation.getDirection(), deployLocation.distance(eyeLocation), FluidCollisionMode.NEVER);
        if (result != null && result.getHitBlock() != null) {
            warnNotEnoughSpace(player);
            return;
        }

        new FunctionChain(
                next -> new AbilityRunnable() {
                    BossBar bossbar;
                    Vex vex;

                    @Override
                    protected synchronized void start() {
                        abilityActive.set(true);
                        player.setCooldown(Material.ENDER_PEARL, info.chargeUp);
                        bossbar = Bukkit.createBossBar("Charging up", BarColor.WHITE, BarStyle.SOLID);
                        bossbar.addPlayer(player);

                        PlayerUtils.consumeEnderPearl(player);

                        vex = player.getWorld().spawn(deployLocation, Vex.class, false, entity -> {
                            entity.setGravity(false);
                            if (entity.getEquipment() != null)
                                entity.getEquipment().setItemInMainHand(new ItemStack(Material.AIR, 0));
                            entity.setAI(false);
                            entity.setSilent(true);
                            entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.codeName, ownerName)));
                        });
                        drone.set(vex);
                    }

                    @Override
                    protected synchronized void tick() {
                        bossbar.setProgress(count / (double) info.chargeUp);
                        vex.getWorld().spawnParticle(Particle.PORTAL, vex.getLocation(), 5, 0.5, 0.5, 0.5, 0.02);
                    }

                    @Override
                    protected synchronized void end() {
                        bossbar.removeAll();
                        if (this.hasCompleted() && vex.isValid())
                            next.run();
                        else {
                            cleanUp();
                        }
                    }
                }.runTaskRepeated(this, 0, 1, info.chargeUp),
                next -> {
                    removeAllNPCs();

                    Location spawn = player.getLocation();
                    spawn.add(0.5, 300.5, 0.5);
                    NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, player.getName());
                    npc.spawn(spawn);
                    npc.getEntity().setGravity(false);
                    npc.getEntity().setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.codeName, ownerName)));
                    npc.setProtected(false);
                    dummy.set(npc);

                    next.run();
                },
                next -> AbilityUtils.delay(this, 10, () -> {
                    NPC npc = dummy.get();
                    npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.HELMET, player.getInventory().getHelmet());
                    npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.CHESTPLATE, player.getInventory().getChestplate());
                    npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.LEGGINGS, player.getInventory().getLeggings());
                    npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.BOOTS, player.getInventory().getBoots());
                    npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.HAND, player.getInventory().getItemInMainHand());
                    next.run();
                }, true),
                next -> AbilityUtils.delay(this, 5, () -> {
                    NPC npc = dummy.get();
                    npc.teleport(npc.getEntity().getLocation().add(0, -300, 0), PlayerTeleportEvent.TeleportCause.PLUGIN);
                    npc.getEntity().setGravity(true);
                    player.teleport(drone.get().getLocation());
                    player.setGameMode(GameMode.SPECTATOR);
                    next.run();
                }, true),
                next -> new AbilityRunnable() {
                    BossBar bossbar;
                    LivingEntity d;

                    @Override
                    protected void start() {
                        bossbar = Bukkit.createBossBar(info.name, BarColor.PURPLE, BarStyle.SOLID);
                        bossbar.addPlayer(player);
                        d = drone.get();
                    }

                    @Override
                    protected void tick() {
                        if (!d.isValid()) {
                            cancel();
                            return;
                        }
                        double maxHealth = EntityUtils.getMaxHealth(d);
                        if (maxHealth > 0)
                            bossbar.setProgress(d.getHealth() / maxHealth);
                        else
                            bossbar.setProgress(0);
                        // d.teleport(player.getEyeLocation());
                        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 1, 0, 0, 0, 0);
                    }

                    @Override
                    protected void end() {
                        bossbar.removeAll();
                        cleanUp();
                        abilityActive.set(false);
                    }
                }.runTaskTimer(this, 0, 1)
        ).execute();
    }
}
