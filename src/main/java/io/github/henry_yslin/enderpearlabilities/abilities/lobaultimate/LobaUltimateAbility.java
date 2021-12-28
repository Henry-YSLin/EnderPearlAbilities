package io.github.henry_yslin.enderpearlabilities.abilities.lobaultimate;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityCouple;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.utils.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class LobaUltimateAbility extends Ability<LobaUltimateAbilityInfo> {

    static final int RADIUS = 10;
    static final List<Material> ALLOWED_BLOCKS = List.of(
            Material.COAL_ORE,
            Material.COPPER_ORE,
            Material.LAPIS_ORE,
            Material.IRON_ORE,
            Material.GOLD_ORE,
            Material.REDSTONE_ORE,
            Material.DIAMOND_ORE,
            Material.EMERALD_ORE,
            Material.DEEPSLATE_COAL_ORE,
            Material.DEEPSLATE_COPPER_ORE,
            Material.DEEPSLATE_LAPIS_ORE,
            Material.DEEPSLATE_IRON_ORE,
            Material.DEEPSLATE_GOLD_ORE,
            Material.DEEPSLATE_REDSTONE_ORE,
            Material.DEEPSLATE_DIAMOND_ORE,
            Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_QUARTZ_ORE,
            Material.NETHER_GOLD_ORE,
            Material.ANCIENT_DEBRIS,
            Material.OBSIDIAN,
            Material.SPONGE,
            Material.WET_SPONGE,
            Material.AMETHYST_BLOCK
    );

    public LobaUltimateAbility(Plugin plugin, LobaUltimateAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    AtomicReference<Shulker> blackMarket = new AtomicReference<>();

    @Override
    public boolean isActive() {
        return abilityActive.get();
    }

    @Override
    public boolean isChargingUp() {
        return chargingUp.get();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (player != null) {
            abilityActive.set(false);
            cooldown.setCooldown(info.getCooldown());
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        super.onPlayerJoin(event);
        Player player = event.getPlayer();
        if (player.getName().equals(ownerName)) {
            abilityActive.set(false);
            cooldown.setCooldown(info.getCooldown());
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!AbilityUtils.verifyAbilityCouple(this, event.getEntity())) return;
        event.getDrops().clear();
    }

    private List<ItemStack> getAllowedBlocks(Location location) {
        return BlockUtils
                .getBlocks(location, RADIUS, block -> ALLOWED_BLOCKS.contains(block.getType())).stream()
                .collect(Collectors.groupingBy(Block::getType, Collectors.counting())).entrySet().stream()
                .map(entry -> new ItemStack(entry.getKey(), Math.toIntExact(entry.getValue())))
                .collect(Collectors.toList());
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!player.getName().equals(ownerName)) return;
        if (!AbilityUtils.verifyAbilityCouple(this, event.getRightClicked())) return;
        if (!abilityActive.get()) return;
        event.setCancelled(true);

        List<ItemStack> allowedBlocks = getAllowedBlocks(event.getRightClicked().getLocation());
        if (allowedBlocks.isEmpty()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No ores found in the area"));
            return;
        }
        InventoryUI ui = new InventoryUI(plugin, "Black Market Boutique", allowedBlocks, item -> {
            if (item.getType() == Material.AIR) return;
            int charge = (int) EntityUtils.getMetadata(event.getRightClicked(), "black-market-charge").orElse(0);
            if (charge <= 0) {
                event.getRightClicked().remove();
                return;
            }
            boolean success = false;
            for (Block block : BlockUtils.getBlocks(event.getRightClicked().getLocation(), RADIUS)) {
                if (block.getType() == item.getType()) {
                    block.setType(Material.AIR);
                    HashMap<Integer, ItemStack> extraItems = player.getInventory().addItem(new ItemStack(item.getType(), 1));
                    if (!extraItems.isEmpty()) {
                        player.getWorld().dropItem(player.getLocation().add(0, 1, 0), new ItemStack(item.getType(), 1));
                    }
                    WorldUtils.spawnParticleCubeOutline(block.getLocation(), block.getLocation().add(1, 1, 1), Particle.DRAGON_BREATH, 2, true);
                    WorldUtils.spawnParticleLine(block.getLocation().add(0.5, 0.5, 0.5), event.getRightClicked().getLocation(), Particle.DRAGON_BREATH, 2, true);
                    success = true;
                    break;
                }
            }
            if (!success) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "That ore no longer exists"));
            }
            if (charge > 1)
                event.getRightClicked().setMetadata("black-market-charge", new FixedMetadataValue(plugin, charge - 1));
            else
                event.getRightClicked().remove();
        }, () -> {
        });
        ui.openInventory(player);
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;

        event.setCancelled(true);

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        if (cooldown.isCoolingDown()) return;
        if (chargingUp.get()) return;
        if (abilityActive.get()) return;

        World world = player.getWorld();

        if (!event.getClickedBlock().getType().isSolid()) return;
        Location location = event.getClickedBlock().getLocation();
        location.add(0.5, 1, 0.5);
        if (location.getBlock().getType() != Material.AIR) return;

        AbilityUtils.consumeEnderPearl(this, player);
        EnderPearlAbilities.getInstance().emitEvent(
                EventListener.class,
                new AbilityActivateEvent(this),
                EventListener::onAbilityActivate
        );

        new FunctionChain(
                next -> {
                    if (blackMarket.get() != null && blackMarket.get().isValid()) {
                        blackMarket.get().remove();
                    }
                    Shulker shulker = world.spawn(location, Shulker.class, false, entity -> {
                        entity.setGravity(false);
                        entity.setGlowing(true);
                        entity.setAI(false);
                        entity.setSilent(true);
                        entity.setColor(DyeColor.CYAN);
                        entity.setMetadata("black-market-charge", new FixedMetadataValue(plugin, 2));
                        entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.getCodeName(), ownerName)));
                    });
                    blackMarket.set(shulker);

                    WorldUtils.spawnParticleCubeOutline(location.clone().add(-RADIUS - 0.5, -RADIUS - 0.5, -RADIUS - 0.5), location.clone().add(RADIUS + 0.5, RADIUS + 0.5, RADIUS + 0.5), Particle.END_ROD, 1, true);
                    world.playSound(shulker.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 2f);

                    next.run();
                },
                next -> new AbilityRunnable() {
                    @Override
                    protected void start() {
                        chargingUp.set(true);
                    }

                    @Override
                    protected void tick() {
                        if (blackMarket.get() == null || !blackMarket.get().isValid()) {
                            cancel();
                            return;
                        }
                        blackMarket.get().setPeek(0.5f * (1 - (count / (float) (info.getChargeUp() / 2))));
                        world.spawnParticle(Particle.FIREWORKS_SPARK, blackMarket.get().getEyeLocation(), 4, 0.1, 0.1, 0.1, 0.02);
                    }

                    @Override
                    protected void end() {
                        chargingUp.set(false);
                        if (this.hasCompleted())
                            next.run();
                        else {
                            blackMarket.get().remove();
                            cooldown.setCooldown(5 * 20);
                        }
                    }
                }.runTaskRepeated(this, 0, 2, info.getChargeUp() / 2),
                next -> new AbilityRunnable() {
                    @Override
                    protected void start() {
                        abilityActive.set(true);
                        blackMarket.get().setPeek(1);
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1, 0);
                    }

                    @Override
                    protected void tick() {
                        if (blackMarket.get() == null || !blackMarket.get().isValid()) {
                            cancel();
                            return;
                        }
                        world.spawnParticle(Particle.END_ROD, blackMarket.get().getEyeLocation(), 4, 0.1, 0.1, 0.1, 0.02);
                    }

                    @Override
                    protected void end() {
                        abilityActive.set(false);
                        blackMarket.get().remove();
                        cooldown.setCooldown(info.getCooldown());
                    }
                }.runTaskRepeated(this, 0, 5, info.getDuration() / 5)
        ).execute();
    }
}
