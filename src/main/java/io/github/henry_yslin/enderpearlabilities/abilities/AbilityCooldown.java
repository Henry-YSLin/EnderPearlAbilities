package io.github.henry_yslin.enderpearlabilities.abilities;

/**
 * Manages the timing, effects and UI of an ability cooldown.
 */
public interface AbilityCooldown {

    /**
     * Get whether the ability can be used at the moment.
     *
     * @return Whether the ability can be used at the moment.
     */
    boolean isAbilityUsable();

    /**
     * Get the remaining ticks until another use of the ability is allowed.
     *
     * @return The remaining ticks.
     */
    int getCooldownTicks();

    /**
     * Cancel the current cooldown, allowing ability use immediately.
     */
    void cancelCooldown();

    /**
     * Set the number of ticks until the next use of the ability is allowed.
     *
     * @param ticks The new cooldown value.
     */
    void setCooldown(int ticks);
}
