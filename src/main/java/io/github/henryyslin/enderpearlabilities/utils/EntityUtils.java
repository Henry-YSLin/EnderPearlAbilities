package io.github.henryyslin.enderpearlabilities.utils;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.MetadataValue;

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
        if (attribute == null) return 0;
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
}