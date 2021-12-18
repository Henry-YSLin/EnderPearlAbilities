package io.github.henry_yslin.enderpearlabilities.abilities.wraithtactical;

import io.github.henry_yslin.enderpearlabilities.abilities.AbilityListener;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.utils.BlockUtils;
import io.github.henry_yslin.enderpearlabilities.utils.ListUtils;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class VoicesFromTheVoidListener extends AbilityListener {

    static final int COOLDOWN = 20;
    static final List<String> SHOOTERS_VOICELINES = List.of(
            "Shooter! Move!",
            "They're aiming right at you.",
            "They are aiming right at you.",
            "You're being aimed at."
    );
    static final List<String> MONSTERS_VOICELINES = List.of(
            "Danger, move!",
            "Monsters, move!",
            "Monster nearby.",
            "They might have seen you.",
            "You're in view of the enemy.",
            "You're being targeted.",
            "You are being targeted.",
            "They are targeting you.",
            "They are hunting you down.",
            "They're targeting you.",
            "They're hunting you down."
    );
    static final List<String> HOT_ZONE_VOICELINES = List.of(
            "You're in a hot zone.",
            "It's not safe there.",
            "You're not safe there.",
            "Many monsters nearby."
    );
    static final List<String> INFESTED_VOICELINES = List.of(
            "Infested. Don't.",
            "It's infested.",
            "Don't. It's infested.",
            "Infested.",
            "Stop. It's infested."
    );
    static final List<String> BLOCK_VOICELINES = List.of(
            "Don't. It's dangerous.",
            "It's a trap.",
            "Stop. It's not safe.",
            "Stop.",
            "Don't.",
            "Trap.",
            "Stop. It's a trap",
            "Don't. It's a trap."
    );
    static final List<EntityType> MONSTERS = List.of(
            EntityType.EVOKER,
            EntityType.VINDICATOR,
            EntityType.PILLAGER,
            EntityType.RAVAGER,
            EntityType.VEX,
            EntityType.ENDERMITE,
            EntityType.HUSK,
            EntityType.PHANTOM,
            EntityType.CREEPER,
            EntityType.MAGMA_CUBE,
            EntityType.SILVERFISH,
            EntityType.SLIME,
            EntityType.ZOMBIE,
            EntityType.ZOMBIE_VILLAGER,
            EntityType.DROWNED,
            EntityType.WITHER_SKELETON,
            EntityType.WITCH,
            EntityType.HOGLIN,
            EntityType.ZOGLIN,
            EntityType.PIGLIN_BRUTE,

            EntityType.SPIDER,
            EntityType.CAVE_SPIDER,
            EntityType.ENDERMAN,
            EntityType.ZOMBIFIED_PIGLIN,
            EntityType.PIGLIN
    );
    static final List<EntityType> SHOOTERS = List.of(
            EntityType.GUARDIAN,
            EntityType.ELDER_GUARDIAN,
            EntityType.SHULKER,
            EntityType.STRAY,
            EntityType.BLAZE,
            EntityType.GHAST,
            EntityType.SKELETON
    );
    static final List<EntityType> ALL_DANGERS = Stream.concat(MONSTERS.stream(), SHOOTERS.stream()).toList();

    private final WraithTacticalAbility ability;

    public VoicesFromTheVoidListener(Plugin plugin, WraithTacticalAbility ability) {
        super(plugin);
        this.ability = ability;
    }

    final AtomicInteger lastWarningTick = new AtomicInteger(0);
    AbilityRunnable warningRunnable;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getName().equals(ability.getOwnerName())) {
            setUpPlayer(player);
        }
    }

    @Override
    public void onEnable() {
        if (ability.getPlayer() != null) {
            setUpPlayer(ability.getPlayer());
        }
    }

    private void setUpPlayer(Player player) {
        if (warningRunnable != null && !warningRunnable.isCancelled())
            warningRunnable.cancel();
        (warningRunnable = new AbilityRunnable() {
            Location lastWarningLocation = null;
            Location lastHotZone = null;

            @Override
            protected void start() {
                super.start();
            }

            private void warn(Location location, String message) {
                if (lastWarningLocation == null || !lastWarningLocation.equals(location)) {
                    lastWarningLocation = location;
                    sendVoice(message);
                }
            }

            @Override
            protected void tick() {
                super.tick();
                if (!player.isValid()) return;
                if (player.getTicksLived() - lastWarningTick.get() < COOLDOWN) return;
                Block block = player.getTargetBlockExact(5, FluidCollisionMode.NEVER);
                if (block == null) return;
                if (block.getType().toString().startsWith("INFESTED")) {
                    warn(block.getLocation(), ListUtils.getRandom(INFESTED_VOICELINES));
                } else if (block.getLocation().equals(player.getLocation().add(0, -1, 0).getBlock().getLocation())) {
                    RayTraceResult result = player.getWorld().rayTraceBlocks(block.getLocation().add(0.5, -0.5, 0.5), new Vector(0, -1, 0), 10, FluidCollisionMode.ALWAYS, true);
                    if (result == null) {
                        warn(block.getLocation(), ListUtils.getRandom(BLOCK_VOICELINES));
                    } else if (result.getHitBlock() != null && result.getHitBlock().getType() == Material.LAVA) {
                        warn(block.getLocation(), ListUtils.getRandom(BLOCK_VOICELINES));
                    }
                } else if (block.getLocation().getY() >= player.getLocation().getY()) {
                    if (BlockUtils.getTouchingBlocks(block.getLocation()).stream().anyMatch(b -> b.getType() == Material.LAVA && b.getY() >= block.getY())) {
                        warn(block.getLocation(), ListUtils.getRandom(BLOCK_VOICELINES));
                    }
                }
                if (player.getTicksLived() - lastWarningTick.get() < COOLDOWN) return;
                if (lastHotZone != null && player.getLocation().distance(lastHotZone) < 15) return;
                if (player.getWorld().getNearbyEntities(player.getLocation(), 10, 10, 10, entity -> ALL_DANGERS.contains(entity.getType())).size() > 5) {
                    sendVoice(ListUtils.getRandom(HOT_ZONE_VOICELINES));
                    lastHotZone = player.getLocation();
                }
            }

            @Override
            protected void end() {
                super.end();
            }
        }).runTaskTimer(this, 0, 5);
    }

    private void sendVoice(String message) {
        Player targetPlayer = ability.getPlayer();
        if (targetPlayer == null) return;
        if (targetPlayer.getTicksLived() - lastWarningTick.get() < COOLDOWN) return;
        ability.getPlayer().sendMessage(ChatColor.GRAY + "<?> " + ChatColor.LIGHT_PURPLE + message);
        lastWarningTick.set(ability.getPlayer().getTicksLived());
    }

    @EventHandler
    public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player player)) return;
        if (!player.getName().equals(ability.getOwnerName())) return;

        if (SHOOTERS.contains(event.getEntity().getType())) {
            sendVoice(ListUtils.getRandom(SHOOTERS_VOICELINES));
        } else if (MONSTERS.contains(event.getEntity().getType())) {
            sendVoice(ListUtils.getRandom(MONSTERS_VOICELINES));
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
