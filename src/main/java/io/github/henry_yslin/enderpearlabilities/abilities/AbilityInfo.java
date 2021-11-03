package io.github.henry_yslin.enderpearlabilities.abilities;

import java.util.Objects;

/**
 * A data class storing the information required to generate a detailed ability description.
 * The {@code codeName} of {@link AbilityInfo} should be constant while other fields can be changed via config.
 */
public final class AbilityInfo {

    public final String codeName;
    public final String name;
    public final String origin;
    public final String description;
    public final String usage;
    public final ActivationHand activation;
    public final int chargeUp;
    public final int duration;
    public final int cooldown;

    private AbilityInfo(String codeName, String name, String origin,
                        String description, String usage,
                        ActivationHand activation, int chargeUp,
                        int duration, int cooldown) {
        this.codeName = codeName;
        this.name = name;
        this.origin = origin;
        this.description = description;
        this.usage = usage;
        this.activation = activation;
        this.chargeUp = chargeUp;
        this.duration = duration;
        this.cooldown = cooldown;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AbilityInfo) obj;
        return Objects.equals(this.codeName, that.codeName) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.origin, that.origin) &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.usage, that.usage) &&
                Objects.equals(this.activation, that.activation) &&
                this.chargeUp == that.chargeUp &&
                this.duration == that.duration &&
                this.cooldown == that.cooldown;
    }

    @Override
    public int hashCode() {
        return Objects.hash(codeName, name, origin, description, usage, activation, chargeUp, duration, cooldown);
    }

    @Override
    public String toString() {
        return "AbilityInfo[" +
                "codeName=" + codeName + ", " +
                "name=" + name + ", " +
                "origin=" + origin + ", " +
                "description=" + description + ", " +
                "usage=" + usage + ", " +
                "activation=" + activation + ", " +
                "chargeUp=" + chargeUp + ", " +
                "duration=" + duration + ", " +
                "cooldown=" + cooldown + ']';
    }

    /**
     * A helper class to construct an {@link AbilityInfo} instance.
     */
    public static class Builder {
        private String codeName;
        private String name;
        private String origin;
        private String description;
        private String usage;
        private ActivationHand activation;
        private int chargeUp;
        private int duration;
        private int cooldown;

        public Builder() {
        }

        /**
         * Shallowly copy fields from another {@link AbilityInfo} instance.
         *
         * @param info The {@link AbilityInfo} instance to copy from.
         */
        public Builder(AbilityInfo info) {
            codeName = info.codeName;
            name = info.name;
            origin = info.origin;
            description = info.description;
            usage = info.usage;
            activation = info.activation;
            chargeUp = info.chargeUp;
            duration = info.duration;
            cooldown = info.cooldown;
        }

        public Builder codeName(String codeName) {
            this.codeName = codeName;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder origin(String origin) {
            this.origin = origin;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder usage(String usage) {
            this.usage = usage;
            return this;
        }

        public Builder activation(ActivationHand activation) {
            this.activation = activation;
            return this;
        }

        public Builder chargeUp(int chargeUp) {
            this.chargeUp = chargeUp;
            return this;
        }

        public Builder duration(int duration) {
            this.duration = duration;
            return this;
        }

        public Builder cooldown(int cooldown) {
            this.cooldown = cooldown;
            return this;
        }

        public AbilityInfo build() {
            return new AbilityInfo(codeName, name, origin, description, usage, activation, chargeUp, duration, cooldown);
        }
    }
}
