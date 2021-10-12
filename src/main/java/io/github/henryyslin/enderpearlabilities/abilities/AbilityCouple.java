package io.github.henryyslin.enderpearlabilities.abilities;

/**
 * A record storing the ability code name and the name of the player activating this ability.
 * This is used to identify the ability instance that an object is related to, usually by setting a {@link org.bukkit.metadata.FixedMetadataValue}.
 */
public record AbilityCouple(String ability, String player) {
}
