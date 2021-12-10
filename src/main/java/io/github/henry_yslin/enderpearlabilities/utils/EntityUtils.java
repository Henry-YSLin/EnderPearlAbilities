package io.github.henry_yslin.enderpearlabilities.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;

public class EntityUtils {

    /**
     * Get maximum health of a LivingEntity.
     *
     * @param entity The entity to query.
     * @return The maximum health of an entity.
     */
    public static double getMaxHealth(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute == null) return Double.POSITIVE_INFINITY;
        return attribute.getValue();
    }

    /**
     * Get the metadata value of a specified key from an entity.
     *
     * @param entity The entity that holds the metadata.
     * @param key    The key for the metadata value.
     * @return The optional metadata value, being empty if the metadata does not exist.
     */
    public static Optional<Object> getMetadata(Entity entity, String key) {
        if (!entity.hasMetadata(key)) return Optional.empty();
        List<MetadataValue> metadata = entity.getMetadata(key);
        if (metadata.size() == 0) return Optional.empty();
        Object value = metadata.get(metadata.size() - 1).value();
        if (value == null) return Optional.empty();
        return Optional.of(value);
    }

    public static void destroyEntityForPlayer(Entity entity, Player player) {
        PacketContainer destroyPacket = EnderPearlAbilities.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        destroyPacket.getIntLists().write(0, List.of(entity.getEntityId()));
        try {
            EnderPearlAbilities.getProtocolManager().sendServerPacket(player, destroyPacket);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    // https://minecraft.fandom.com/wiki/Entity#Motion_of_entities
    public static double getGravity(Entity entity) {
        if (!entity.hasGravity()) return 0;
        if (entity instanceof LivingEntity livingEntity) {
            if (livingEntity.hasPotionEffect(PotionEffectType.SLOW_FALLING)) return 0.01;
            else return 0.08;
        }
        if (entity.getType() == EntityType.DROPPED_ITEM
                || entity.getType() == EntityType.FALLING_BLOCK
                || entity.getType() == EntityType.PRIMED_TNT)
            return 0.04;
        if (entity instanceof Minecart) return 0.04;
        if (entity instanceof Boat) return 0.04;
        if (entity.getType() == EntityType.LLAMA_SPIT) return 0.06;
        if (entity.getType() == EntityType.EXPERIENCE_ORB) return 0.03;
        if (entity.getType() == EntityType.FISHING_HOOK) return 0.03;
        if (entity instanceof Projectile) {
            if (entity instanceof Arrow || entity instanceof Trident)
                return 0.05;
            if (entity instanceof Fireball) return 0.1;
            return 0.03;
        }
        return 0.03;
    }

    public static double getDrag(Entity entity) {
        if (entity instanceof LivingEntity) return 0.02;
        if (entity.getType() == EntityType.DROPPED_ITEM
                || entity.getType() == EntityType.FALLING_BLOCK
                || entity.getType() == EntityType.PRIMED_TNT)
            return 0.02;
        if (entity instanceof Minecart) return 0.05;
        if (entity instanceof Boat) return 0;
        if (entity.getType() == EntityType.LLAMA_SPIT) return 0.01;
        if (entity.getType() == EntityType.EXPERIENCE_ORB) return 0.02;
        if (entity.getType() == EntityType.FISHING_HOOK) return 0.08;
        if (entity instanceof Projectile) {
            if (entity instanceof Arrow || entity instanceof Trident)
                return 0.01;
            if (entity instanceof Fireball) {
                if (entity instanceof WitherSkull skull)
                    if (skull.isCharged())
                        return 0.27;
                return 0.05;
            }
            return 0.01;
        }
        return 0.02;
    }

    public static boolean isFlying(Entity entity) {
        if (entity instanceof Player player)
            return player.isFlying() || player.isGliding();
        if (entity.getType() == EntityType.PHANTOM || entity.getType() == EntityType.BAT || entity.getType() == EntityType.ENDER_DRAGON)
            return true;
        if (entity.getType() == EntityType.BLAZE && !entity.isOnGround())
            return true;
        return false;
    }

    public static boolean hasDelayedDrag(Entity entity) {
        if (entity instanceof LivingEntity) return true;
        if (entity instanceof LargeFireball) return true;
        return false;
    }
}
