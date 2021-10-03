package io.github.henryyslin.enderpearlabilities.mirage;

import io.github.henryyslin.enderpearlabilities.Ability;
import io.github.henryyslin.enderpearlabilities.AbilityCooldown;
import io.github.henryyslin.enderpearlabilities.ActivationHand;
import io.github.henryyslin.enderpearlabilities.utils.*;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.api.trait.trait.Equipment;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AbilityMirage implements Ability {
    public String getName() {
        return "Life of the Party";
    }

    public String getOrigin() {
        return "Apex - Mirage";
    }

    public String getConfigName() {
        return "mirage";
    }

    public String getDescription() {
        return "Deploy a team of decoys to distract enemies and protect the player. Decoys last for a set amount of time and use tools on their hands with reduced efficiency.";
    }

    public ActivationHand getActivation() {
        return ActivationHand.OffHand;
    }

    public int getChargeUp() {
        return 40;
    }

    public int getDuration() {
        return 1000;
    }

    public int getCooldown() {
        return 800;
    }

    final Plugin plugin;
    final FileConfiguration config;
    final String ownerName;
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    AbilityCooldown cooldown;

    public AbilityMirage(Plugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
        this.ownerName = config.getString(getConfigName());
    }

    private void removeAllNPCs() {
        CitizensAPI.getNPCRegistries().forEach(registry -> registry.forEach(npc -> {
            if (npc.isSpawned()) {
                npc.getEntity().getWorld().spawnParticle(Particle.ASH, npc.getEntity().getLocation(), 20, 0.5, 1, 0.5, 0.02);
            }
        }));
        CitizensAPI.getNPCRegistries().forEach(NPCRegistry::deregisterAll);
    }

    private int countNPCs() {
        AtomicInteger count = new AtomicInteger();
        CitizensAPI.getNPCRegistries().forEach(registry -> registry.forEach(npc -> {
            if (npc.isSpawned()) count.getAndIncrement();
        }));
        return count.get();
    }

    @Override
    public void onEnable() {
        removeAllNPCs();
        CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(CloneTrait.class).withName("cloneTrait"));
    }

    @Override
    public void onDisable() {
        removeAllNPCs();
        CitizensAPI.getTraitFactory().deregisterTrait(TraitInfo.create(CloneTrait.class).withName("cloneTrait"));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getName().equals(ownerName)) {
            removeAllNPCs();
            abilityActive.set(false);
            cooldown = new AbilityCooldown(plugin, player);
            cooldown.startCooldown(getCooldown());
        }
    }

    @EventHandler
    public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        Entity target = event.getTarget();
        Entity entity = event.getEntity();
        if (!(entity instanceof Mob mob)) return;
        if (target == null) return;
        if (target.hasMetadata("NPC")) return;
        if (!(target instanceof Player player)) return;
        if (!player.getName().equals(ownerName)) return;
        if (entity.hasMetadata("NPC")) return;
        ArrayList<LivingEntity> candidates = new ArrayList<>();
        CitizensAPI.getNPCRegistry().forEach(npc -> {
            if (npc.isSpawned()) candidates.add((LivingEntity) npc.getEntity());
        });
        if (candidates.isEmpty()) return;
        mob.setTarget(ListUtils.getRandom(candidates));
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!damager.hasMetadata("NPC")) return;
        event.setDamage(event.getDamage() * 3);
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, getActivation())) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;

        if (abilityActive.get()) return;

        ArrayList<NPC> npcs = new ArrayList<>();

        new FunctionChain(
                next -> {
                    abilityActive.set(true);
                    if (player.getGameMode() != GameMode.CREATIVE) {
                        player.getInventory().removeItem(new ItemStack(Material.ENDER_PEARL, 1));
                    }
                    next.invoke();
                },
                next -> AbilityUtils.chargeUpSequence(plugin, player, getChargeUp(), next),
                next -> {
                    World world = player.getWorld();
                    world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 0);

                    removeAllNPCs();

                    List<Block> spawnLocations = BlockUtils.getSafeSpawningBlocks(player.getLocation(), 4);

                    if (spawnLocations.isEmpty()) {
                        abilityActive.set(false);
                        cooldown.cancelCooldown();
                        player.sendMessage(ChatColor.RED + "No place to spawn clones!");
                        plugin.getLogger().info("[" + getName() + "] No valid spawn locations at " + player.getLocation() + ". Cancelling.");
                        return;
                    }

                    for (int i = 0; i < 5; i++) {
                        Location spawn = ListUtils.getRandom(spawnLocations).getLocation();
                        spawn.add(0.5, 300.5, 0.5);
                        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, player.getName());
                        npc.spawn(spawn);
                        npc.getEntity().setGravity(false);
                        npc.setProtected(false);
                        npcs.add(npc);
                    }

                    next.invoke();
                },
                next -> new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (NPC npc : npcs) {
                            npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.HELMET, player.getInventory().getHelmet());
                            npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.CHESTPLATE, player.getInventory().getChestplate());
                            npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.LEGGINGS, player.getInventory().getLeggings());
                            npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.BOOTS, player.getInventory().getBoots());
                            npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.HAND, player.getInventory().getItemInMainHand());
                        }
                        next.invoke();
                    }
                }.runTaskLater(plugin, 10),
                next -> new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (NPC npc : npcs) {
                            npc.teleport(npc.getEntity().getLocation().add(0, -300, 0), PlayerTeleportEvent.TeleportCause.PLUGIN);
                            npc.getEntity().setGravity(true);
                            CloneTrait trait = npc.getOrAddTrait(CloneTrait.class);
                            trait.toggle(player, true);
                        }
                        next.invoke();
                    }
                }.runTaskLater(plugin, 5),
                next -> new AdvancedRunnable() {
                    BossBar bossbar;
                    Score score;

                    @Override
                    protected synchronized void start() {
                        bossbar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + getName(), BarColor.PURPLE, BarStyle.SOLID);
                        bossbar.addPlayer(player);
                        Objective obj = player.getScoreboard().getObjective("Clones");
                        if (obj == null)
                            obj = player.getScoreboard().registerNewObjective("Clones", "dummy", "Clones");
                        score = obj.getScore(ChatColor.GRAY + "Clones left");
                        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
                        score.setScore(countNPCs());
                    }

                    @Override
                    protected synchronized void tick() {
                        if (!abilityActive.get()) {
                            cancel();
                            return;
                        }
                        bossbar.setProgress(count * 10 / (double) getDuration());
                        int count = countNPCs();
                        score.setScore(count);
                        if (count == 0) cancel();
                    }

                    @Override
                    protected synchronized void end() {
                        bossbar.removeAll();
                        removeAllNPCs();
                        player.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
                        abilityActive.set(false);
                        cooldown.startCooldown(getCooldown());
                        next.invoke();
                    }
                }.runTaskRepeated(plugin, 0, 10, getDuration() / 10)
        ).execute();
    }
}
