package io.github.henryyslin.enderpearlabilities.abilities.seer;

import io.github.henryyslin.enderpearlabilities.utils.AbilityRunnable;
import io.github.henryyslin.enderpearlabilities.utils.EntityUtils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.Arrays;
import java.util.List;

public class HeartSeekerRunnable extends AbilityRunnable {

    static final double SCAN_RANGE = 36;
    static final double RAY_SIZE = 2;

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

    private void playHeartbeat(Player player, double distance) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, (float) Math.max((1 - distance / SCAN_RANGE) * 0.5f, 0.05f), 1.5f);
    }

    private RayTraceResult rayTrace(double raySize) {
        return player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), SCAN_RANGE, raySize, entity -> {
            if (entity instanceof Player p) {
                if (p.getGameMode() == GameMode.SPECTATOR) return false;
            }
            return !entity.equals(player) && entity instanceof LivingEntity;
        });
    }

    @Override
    protected void tick() {
        if (!player.isSneaking()) return;
        RayTraceResult result = rayTrace(0);
        if (result == null || result.getHitEntity() == null)
            result = rayTrace(RAY_SIZE);
        if (result != null && result.getHitEntity() != null) {
            LivingEntity livingEntity = (LivingEntity) result.getHitEntity();
            double maxHealth = EntityUtils.getMaxHealth(livingEntity);
            String heartbeat = "♥";
            double distance = livingEntity.getLocation().distance(player.getLocation());
            int trackTick = livingEntity.getTicksLived() / 5;
            if (livingEntity.isValid() && !UNDEAD_MOBS.contains(livingEntity.getType())) {
                if (maxHealth == 0) {
                    if (livingEntity.getHealth() / maxHealth > 0.5) {
                        if (trackTick % 4 == 0) {
                            heartbeat = ChatColor.WHITE + heartbeat;
                            playHeartbeat(player, distance);
                        }
                    } else if (livingEntity.getHealth() / maxHealth > 0.25) {
                        if (trackTick % 3 == 0) {
                            heartbeat = ChatColor.WHITE + heartbeat;
                            playHeartbeat(player, distance);
                        }
                    } else {
                        if (trackTick % 2 == 0) {
                            heartbeat = ChatColor.WHITE + heartbeat;
                            playHeartbeat(player, distance);
                        }
                    }
                } else if (trackTick % 4 == 0) {
                    heartbeat = ChatColor.WHITE + heartbeat;
                    playHeartbeat(player, distance);
                }
            } else {
                heartbeat = ChatColor.GRAY + heartbeat;
            }
            if (maxHealth == 0)
                player.sendTitle(" ", String.format(ChatColor.BLUE + "%.1fm %.1f/%.1f%s", distance, livingEntity.getHealth(), maxHealth, heartbeat), 0, 8, 10);
            else
                player.sendTitle(" ", String.format(ChatColor.BLUE + "%.1fm %.1f%s", distance, livingEntity.getHealth(), heartbeat), 0, 8, 10);
        }
    }
}
