package io.github.henry_yslin.enderpearlabilities.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

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
}
