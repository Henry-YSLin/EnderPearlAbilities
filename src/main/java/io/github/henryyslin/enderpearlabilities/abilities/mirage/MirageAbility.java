package io.github.henryyslin.enderpearlabilities.abilities.mirage;

import io.github.henryyslin.enderpearlabilities.abilities.Ability;
import io.github.henryyslin.enderpearlabilities.abilities.AbilityCouple;
import io.github.henryyslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henryyslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henryyslin.enderpearlabilities.utils.*;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.api.trait.trait.Equipment;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MirageAbility extends Ability {

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 40);
        config.addDefault("duration", 1000);
        config.addDefault("cooldown", 800);
    }

    public MirageAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("mirage")
                .name("Life of the Party")
                .origin("Apex - Mirage")
                .description("Deploy a team of decoys to distract enemies and protect the player. Decoys last for a set amount of time and use tools on their hands with reduced efficiency.")
                .activation(ActivationHand.OffHand);

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

    private void removeAllNPCs() {
        CitizensAPI.getNPCRegistries().forEach(registry -> {
            List<NPC> toBeRemoved = new ArrayList<>();
            registry.forEach(npc -> {
                if (npc.isSpawned()) {
                    if (!npc.getEntity().hasMetadata("ability")) return;
                    if (!AbilityUtils.verifyAbilityCouple(this, npc.getEntity())) return;
                    npc.getEntity().getWorld().spawnParticle(Particle.ASH, npc.getEntity().getLocation(), 20, 0.5, 1, 0.5, 0.02);
                }
                toBeRemoved.add(npc);
            });
            toBeRemoved.forEach(registry::deregister);
        });
    }

    private int countNPCs() {
        AtomicInteger count = new AtomicInteger();
        CitizensAPI.getNPCRegistries().forEach(registry -> registry.forEach(npc -> {
            if (!npc.isSpawned()) return;
            if (!npc.getEntity().hasMetadata("ability")) return;
            if (!AbilityUtils.verifyAbilityCouple(this, npc.getEntity())) return;
            count.getAndIncrement();
        }));
        return count.get();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        removeAllNPCs();
        if (player != null) {
            abilityActive.set(false);
            cooldown.startCooldown(info.cooldown);
        }
        if (CitizensAPI.getTraitFactory().getRegisteredTraits().stream().anyMatch(traitInfo -> traitInfo.getTraitName().equals("clonetrait")))
            CitizensAPI.getTraitFactory().deregisterTrait(TraitInfo.create(CloneTrait.class).withName("clonetrait"));
        CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(CloneTrait.class).withName("clonetrait"));
    }

    @Override
    public void onDisable() {
        super.onDisable();
        removeAllNPCs();
        if (CitizensAPI.getTraitFactory().getRegisteredTraits().stream().anyMatch(traitInfo -> traitInfo.getTraitName().equals("clonetrait")))
            CitizensAPI.getTraitFactory().deregisterTrait(TraitInfo.create(CloneTrait.class).withName("clonetrait"));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        super.onPlayerJoin(event);
        Player player = event.getPlayer();
        if (player.getName().equals(ownerName)) {
            removeAllNPCs();
            abilityActive.set(false);
            cooldown.startCooldown(info.cooldown);
        }
    }

    @EventHandler
    public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        Entity target = event.getTarget();
        Entity entity = event.getEntity();
        if (!(entity instanceof Mob mob)) return;
        if (target == null) return;
        if (AbilityUtils.verifyAbilityCouple(this, target)) return;
        if (!(target instanceof Player player)) return;
        if (!player.getName().equals(ownerName)) return;
        if (AbilityUtils.verifyAbilityCouple(this, entity)) return;
        ArrayList<LivingEntity> candidates = new ArrayList<>();
        CitizensAPI.getNPCRegistry().forEach(npc -> {
            if (!npc.isSpawned()) return;
            if (!AbilityUtils.verifyAbilityCouple(this, npc.getEntity())) return;
            candidates.add((LivingEntity) npc.getEntity());
        });
        if (candidates.isEmpty()) return;
        mob.setTarget(ListUtils.getRandom(candidates));
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!AbilityUtils.verifyAbilityCouple(this, damager)) return;
        event.setDamage(event.getDamage() * 3);
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;

        if (abilityActive.get()) return;

        ArrayList<NPC> npcs = new ArrayList<>();

        new FunctionChain(
                next -> {
                    abilityActive.set(true);
                    AbilityUtils.consumeEnderPearl(player);
                    next.run();
                },
                next -> AbilityUtils.chargeUpSequence(this, player, info.chargeUp, next),
                next -> {
                    World world = player.getWorld();
                    world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 0);

                    removeAllNPCs();

                    List<Block> spawnLocations = BlockUtils.getBlocks(player.getLocation(), 4, BlockUtils::isSafeSpawningBlock);

                    if (spawnLocations.isEmpty()) {
                        abilityActive.set(false);
                        cooldown.cancelCooldown();
                        player.sendMessage(ChatColor.RED + "No place to spawn clones!");
                        plugin.getLogger().info("[" + info.name + "] No valid spawn locations at " + player.getLocation() + ". Cancelling.");
                        return;
                    }

                    for (int i = 0; i < 5; i++) {
                        Location spawn = ListUtils.getRandom(spawnLocations).getLocation();
                        spawn.add(0.5, 300.5, 0.5);
                        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, player.getName());
                        npc.spawn(spawn);
                        npc.getEntity().setGravity(false);
                        npc.getEntity().setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.codeName, ownerName)));
                        npc.setProtected(false);
                        npcs.add(npc);
                    }

                    next.run();
                },
                next -> new AbilityRunnable() {
                    @Override
                    public void tick() {
                        for (NPC npc : npcs) {
                            npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.HELMET, player.getInventory().getHelmet());
                            npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.CHESTPLATE, player.getInventory().getChestplate());
                            npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.LEGGINGS, player.getInventory().getLeggings());
                            npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.BOOTS, player.getInventory().getBoots());
                            npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.HAND, player.getInventory().getItemInMainHand());
                        }
                        next.run();
                    }
                }.runTaskLater(this, 10),
                next -> new AbilityRunnable() {
                    @Override
                    public void tick() {
                        for (NPC npc : npcs) {
                            npc.teleport(npc.getEntity().getLocation().add(0, -300, 0), PlayerTeleportEvent.TeleportCause.PLUGIN);
                            npc.getEntity().setGravity(true);
                            CloneTrait trait = npc.getOrAddTrait(CloneTrait.class);
                            trait.toggle(player, true);
                        }
                        next.run();
                    }
                }.runTaskLater(this, 5),
                next -> new AbilityRunnable() {
                    BossBar bossbar;
                    Score score;

                    @Override
                    protected synchronized void start() {
                        bossbar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.name, BarColor.PURPLE, BarStyle.SOLID);
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
                        bossbar.setProgress(count * 10 / (double) info.duration);
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
                        cooldown.startCooldown(info.cooldown);
                        next.run();
                    }
                }.runTaskRepeated(this, 0, 10, info.duration / 10)
        ).execute();
    }
}
