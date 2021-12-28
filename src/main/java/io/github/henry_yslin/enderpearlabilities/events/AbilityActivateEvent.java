package io.github.henry_yslin.enderpearlabilities.events;

import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import org.jetbrains.annotations.NotNull;

public final class AbilityActivateEvent extends AbilityEvent {

    public AbilityActivateEvent(@NotNull Ability<?> ability) {
        super(ability);
    }
}
