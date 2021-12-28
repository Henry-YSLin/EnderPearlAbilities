package io.github.henry_yslin.enderpearlabilities.events;

import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import org.jetbrains.annotations.NotNull;

public abstract class AbilityEvent extends Event {
    protected Ability<?> ability;

    public AbilityEvent(@NotNull final Ability<?> ability) {
        this.ability = ability;
    }

    /**
     * Returns the ability involved in this event
     *
     * @return Ability that is involved in this event
     */
    @NotNull
    public final Ability<?> getAbility() {
        return ability;
    }
}

