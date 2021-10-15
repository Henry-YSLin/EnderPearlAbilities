package io.github.henryyslin.enderpearlabilities.abilities.seer;

import io.github.henryyslin.enderpearlabilities.utils.AbilityRunnable;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.Arrays;
import java.util.List;

public class HeartSeekerRunnable extends AbilityRunnable {

    static final double SCAN_RANGE = 36;
    static final double RAY_SIZE = 1;

    static final List<EntityType> UNDEAD_MOBS = Arrays.stream(new EntityType[]{
            EntityType.DROWNED,
            EntityType.HUSK,
            EntityType.ZOMBIE_VILLAGER,
            EntityType.PHANTOM,
            EntityType.SKELETON,
            EntityType.SKELETON_HORSE,
            EntityType.STRAY,
            EntityType.WITHER,
            EntityType.WITHER_SKELETON,
            EntityType.ZOGLIN,
            EntityType.ZOMBIE,
            EntityType.ZOMBIE_HORSE,
            EntityType.ZOMBIFIED_PIGLIN
    }).toList();

    Player player;

    public HeartSeekerRunnable(Player player) {
        this.player = player;
    }

    private void playHeartbeat(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.3f, 1);
    }

    @Override
    protected void tick() {
        if (!player.isSneaking()) return;
        RayTraceResult result = player.getWorld().rayTraceEntities(player.getLocation(), player.getLocation().getDirection(), SCAN_RANGE, RAY_SIZE, entity -> !entity.equals(player) && entity instanceof LivingEntity);
        if (result != null && result.getHitEntity() != null) {
            LivingEntity livingEntity = (LivingEntity) result.getHitEntity();
            AttributeInstance attribute = livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            String heartbeat = "♥";
            int trackTick = livingEntity.getTicksLived() / 5;
            if (livingEntity.isValid() && !UNDEAD_MOBS.contains(livingEntity.getType())) {
                if (attribute != null) {
                    if (livingEntity.getHealth() / attribute.getValue() > 0.5) {
                        if (trackTick % 4 == 0) {
                            heartbeat = ChatColor.WHITE + heartbeat;
                            playHeartbeat(player);
                        }
                    } else if (livingEntity.getHealth() / attribute.getValue() > 0.25) {
                        if (trackTick % 3 == 0) {
                            heartbeat = ChatColor.WHITE + heartbeat;
                            playHeartbeat(player);
                        }
                    } else {
                        if (trackTick % 2 == 0) {
                            heartbeat = ChatColor.WHITE + heartbeat;
                            playHeartbeat(player);
                        }
                    }
                } else if (trackTick % 4 == 0) {
                    heartbeat = ChatColor.WHITE + heartbeat;
                    playHeartbeat(player);
                }
            } else {
                heartbeat = ChatColor.GRAY + heartbeat;
            }
            if (attribute != null)
                player.sendTitle(" ", String.format(ChatColor.BLUE + "%.1fm %.1f/%.1f%s", livingEntity.getLocation().distance(player.getLocation()), livingEntity.getHealth(), attribute.getValue(), heartbeat), 0, 8, 10);
            else
                player.sendTitle(" ", String.format(ChatColor.BLUE + "%.1fm %.1f%s", livingEntity.getLocation().distance(player.getLocation()), livingEntity.getHealth(), heartbeat), 0, 8, 10);
        }
    }
}
