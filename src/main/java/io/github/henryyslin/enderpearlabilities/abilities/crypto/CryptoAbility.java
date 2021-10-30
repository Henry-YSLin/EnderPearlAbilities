package io.github.henryyslin.enderpearlabilities.abilities.crypto;

import io.github.henryyslin.enderpearlabilities.abilities.Ability;
import io.github.henryyslin.enderpearlabilities.abilities.AbilityCouple;
import io.github.henryyslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henryyslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henryyslin.enderpearlabilities.utils.*;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
                .description("Deploys an aerial drone for various purposes. Cooldown is only active if the drone is destroyed or recalled.")
                .usage("Right click to deploy a new drone or enter an existing one. Sneak while right-clicking to recall a deployed drone. Length of cooldown depends on the drone's heath. Right click while in the drone to interact with objects. Left click to exit drone view and leave the drone in place. Ramming the drone into entities will cause damage to both the drone and the entity. Driving the drone through walls will temporarily limit vision.")
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

    final AtomicInteger rightClickCooldown = new AtomicInteger(0);
    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    final AtomicReference<LivingEntity> drone = new AtomicReference<>();
    final AtomicReference<NPC> dummy = new AtomicReference<>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        super.onPlayerJoin(event);
        exitDrone();
        Player player = event.getPlayer();
        if (player.getName().equals(ownerName)) {
            setUpPlayer(player);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        removeDrone();
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

    private LivingEntity spawnDrone(Location deployLocation) {
        if (deployLocation.getWorld() == null) {
            plugin.getLogger().warning("Trying to spawn drone with null world.");
            return null;
        }
        if (isDroneValid())
            drone.get().remove();
        Vex vex = deployLocation.getWorld().spawn(deployLocation, Vex.class, false, entity -> {
            entity.setGravity(false);
            if (entity.getEquipment() != null)
                entity.getEquipment().setItemInMainHand(new ItemStack(Material.AIR, 0));
            entity.setAI(false);
            entity.setPersistent(true);
            entity.setSilent(true);
            entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.codeName, ownerName)));
        });
        drone.set(vex);
        return vex;
    }

    private void enterDrone(Player player) {
        if (drone.get() == null) {
            plugin.getLogger().warning("Trying to enter null drone.");
            return;
        }

        spawnDrone(drone.get().getLocation());
        EntityUtils.destroyEntityForPlayer(drone.get(), player);

        removeAllNPCs();

        Location spawn = player.getLocation();
        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, player.getName());
        npc.spawn(spawn);
        LivingEntity entity = (LivingEntity) npc.getEntity();
        entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.codeName, ownerName)));
        entity.setHealth(player.getHealth());
        npc.setProtected(false);
        dummy.set(npc);
        npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.HELMET, player.getInventory().getHelmet());
        npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.CHESTPLATE, player.getInventory().getChestplate());
        npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.LEGGINGS, player.getInventory().getLeggings());
        npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.BOOTS, player.getInventory().getBoots());
        npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.HAND, player.getInventory().getItemInMainHand());
        npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.OFF_HAND, player.getInventory().getItemInOffHand());

        //npc.teleport(player.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        npc.getEntity().setGravity(true);
        player.teleport(drone.get().getLocation().add(0, -player.getEyeHeight(), 0));
        player.setGameMode(GameMode.SPECTATOR);
    }

    private void exitDrone() {
        removeAllNPCs();

        if (player != null && player.getGameMode() == GameMode.SPECTATOR) {
            LivingEntity d = drone.get();
            if (d != null && d.isValid()) {
                d.teleport(player.getEyeLocation().add(0, -d.getEyeHeight(), 0));
            }
        }

        NPC n = dummy.get();
        if (n != null) {
            if (player != null) {
                player.setGameMode(GameMode.SURVIVAL);
                player.teleport(n.getStoredLocation());
            }
            n.getOwningRegistry().deregister(n);
            dummy.set(null);
        }

        if (isDroneValid()) {
            spawnDrone(drone.get().getLocation());
        }
    }

    private void removeDrone() {
        if (abilityActive.get()) exitDrone();

        LivingEntity d = drone.get();
        if (d != null && d.isValid()) {
            d.remove();
            drone.set(null);
        }
    }

    private boolean isDroneValid() {
        return drone.get() != null && drone.get().isValid();
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
        removeDrone();
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) return;
        if (!abilityActive.get()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        if (abilityActive.get())
            exitDrone();
    }

    @EventHandler
    public void onEntityDropItem(EntityDropItemEvent event) {
        if (!AbilityUtils.verifyAbilityCouple(this, event.getEntity())) return;
        event.getItemDrop().remove();
    }

    private void warnNotEnoughSpace(Player player) {
        player.sendTitle(" ", ChatColor.LIGHT_PURPLE + "Not enough space", 5, 20, 10);
    }

    private void damageEffect(Player player) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + String.format("! %.0f❤ !", player.getHealth())));
        player.addPotionEffect(PotionEffectType.BLINDNESS.createEffect(17, 1));
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        NPC npc = dummy.get();
        if (npc != null && event.getEntity().equals(npc.getEntity())) {
            player.damage(event.getDamage());
            player.setLastDamageCause(event);
            LivingEntity livingNpc = (LivingEntity) npc.getEntity();
            player.setHealth(Math.ceil(livingNpc.getHealth()));
            damageEffect(player);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!event.getEntity().equals(player)) return;
        if (!abilityActive.get()) return;
        removeDrone();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().equals(drone.get())) {
            cooldown.startCooldown(info.cooldown);
            player.sendTitle(" ", ChatColor.LIGHT_PURPLE + "Drone destroyed", 5, 30, 20);
        } else {
            NPC npc = dummy.get();
            if (npc != null && !npc.isSpawned() && AbilityUtils.verifyAbilityCouple(this, event.getEntity())) {
                player.setHealth(event.getEntity().getHealth());
                damageEffect(player);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (player.equals(this.player) && abilityActive.get()) {
            if (event.getAction() == Action.LEFT_CLICK_AIR) {
                if (player.getTicksLived() - rightClickCooldown.get() < 2) return;
                abilityActive.set(false);
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                rightClickCooldown.set(player.getTicksLived());
                Block block = event.getClickedBlock();

                PlayerUtils.rightClickBlock(player, block);
            }
            return;
        }

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;
        if (chargingUp.get()) return;
        if (abilityActive.get()) return;

        if (player.isSneaking()) {
            if (isDroneValid()) {
                new AbilityRunnable() {
                    BossBar bossbar;

                    @Override
                    protected synchronized void start() {
                        chargingUp.set(true);
                        player.setCooldown(Material.ENDER_PEARL, info.chargeUp);
                        bossbar = Bukkit.createBossBar("Recalling drone", BarColor.WHITE, BarStyle.SOLID);
                        bossbar.addPlayer(player);
                    }

                    @Override
                    protected synchronized void tick() {
                        if (!isDroneValid()) {
                            cancel();
                            return;
                        }
                        bossbar.setProgress(count / (double) info.chargeUp);
                        drone.get().getWorld().spawnParticle(Particle.PORTAL, drone.get().getLocation(), 5, 0.5, 0.5, 0.5, 0.02);
                    }

                    @Override
                    protected synchronized void end() {
                        bossbar.removeAll();
                        chargingUp.set(false);
                        if (isDroneValid()) {
                            double maxHealth = EntityUtils.getMaxHealth(drone.get());
                            double damage = maxHealth - drone.get().getHealth();
                            cooldown.startCooldown((int) (info.cooldown / maxHealth * damage));
                            removeDrone();
                        }
                    }
                }.runTaskRepeated(this, 0, 1, info.chargeUp);
            } else {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("No existing drone to recall."));
            }
            return;
        }

        World world = player.getWorld();

        Location deployLocation = player.getEyeLocation();
        if (!isDroneValid()) {
            // Check if there's enough space to deploy drone
            Location eyeLocation = player.getEyeLocation();
            deployLocation = eyeLocation.clone().add(eyeLocation.getDirection().multiply(2));
            if (deployLocation.getBlock().getType() != Material.AIR) {
                warnNotEnoughSpace(player);
                return;
            }
            RayTraceResult result = world.rayTraceBlocks(eyeLocation, eyeLocation.getDirection(), deployLocation.distance(eyeLocation), FluidCollisionMode.NEVER);
            if (result != null && result.getHitBlock() != null) {
                warnNotEnoughSpace(player);
                return;
            }
        }

        Location finalDeployLocation = deployLocation;

        new FunctionChain(
                next -> new AbilityRunnable() {
                    BossBar bossbar;
                    LivingEntity vex;

                    @Override
                    protected synchronized void start() {
                        chargingUp.set(true);
                        player.setCooldown(Material.ENDER_PEARL, info.chargeUp);
                        bossbar = Bukkit.createBossBar("Charging up", BarColor.WHITE, BarStyle.SOLID);
                        bossbar.addPlayer(player);

                        PlayerUtils.consumeEnderPearl(player);

                        if (isDroneValid())
                            vex = drone.get();
                        else
                            vex = spawnDrone(finalDeployLocation);
                    }

                    @Override
                    protected synchronized void tick() {
                        bossbar.setProgress(count / (double) info.chargeUp);
                        vex.getWorld().spawnParticle(Particle.PORTAL, vex.getLocation(), 5, 0.5, 0.5, 0.5, 0.02);
                    }

                    @Override
                    protected synchronized void end() {
                        bossbar.removeAll();
                        chargingUp.set(false);
                        if (this.hasCompleted() && vex.isValid())
                            next.run();
                        else {
                            removeDrone();
                        }
                    }
                }.runTaskRepeated(this, 0, 1, info.chargeUp),
                next -> {
                    enterDrone(player);
                    abilityActive.set(true);
                    next.run();
                },
                next -> new AbilityRunnable() {
                    BossBar bossbar;
                    LivingEntity d;
                    int crosshairInterval = 0;

                    @Override
                    protected void start() {
                        bossbar = Bukkit.createBossBar(info.name, BarColor.PURPLE, BarStyle.SOLID);
                        bossbar.addPlayer(player);
                        d = drone.get();
                    }

                    @Override
                    protected void tick() {
                        if (!d.isValid() || !abilityActive.get()) {
                            cancel();
                            return;
                        }
                        double maxHealth = EntityUtils.getMaxHealth(d);
                        if (maxHealth > 0)
                            bossbar.setProgress(d.getHealth() / maxHealth);
                        else
                            bossbar.setProgress(0);
                        d.teleport(player.getEyeLocation().add(0, -d.getEyeHeight(), 0));
                        if (crosshairInterval <= 0) {
                            crosshairInterval = 10;
                            player.sendTitle(" ", "^", 0, 10, 5);
                        }
                        crosshairInterval--;
                        RayTraceResult result = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), 0.1, FluidCollisionMode.NEVER, true);
                        if (result != null && result.getHitBlock() != null) {
                            Material block = result.getHitBlock().getType();
                            if (block.isSolid() && block.isOccluding())
                                player.addPotionEffect(PotionEffectType.BLINDNESS.createEffect(30, 1));
                        }
                        RayTraceResult result2 = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 0.1, entity -> entity instanceof LivingEntity && !entity.equals(player) && !entity.equals(d));
                        if (result2 != null && result2.getHitEntity() != null) {
                            LivingEntity victim = (LivingEntity) result2.getHitEntity();
                            victim.damage(2, d);
                            d.damage(1, victim);
                            player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_BEE_HURT, 0.5f, 2);
                        }
                        player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_BEE_LOOP, 0.05f, 2);
                        player.getWorld().spawnParticle(Particle.END_ROD, player.getEyeLocation().add(0, -0.3, 0), 1, 0, 0, 0, 0);
                    }

                    @Override
                    protected void end() {
                        bossbar.removeAll();
                        if (d.isValid())
                            exitDrone();
                        else
                            removeDrone();
                        abilityActive.set(false);
                    }
                }.runTaskTimer(this, 0, 1)
        ).execute();
    }
}
