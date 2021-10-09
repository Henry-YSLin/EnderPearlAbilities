package io.github.henryyslin.enderpearlabilities.abilities;

import java.util.Objects;

public final class AbilityInfo {
    public final String codeName;
    public final String name;
    public final String origin;
    public final String description;
    public final ActivationHand activation;
    public final int chargeUp;
    public final int duration;
    public final int cooldown;

    private AbilityInfo(String codeName, String name, String origin,
                        String description,
                        ActivationHand activation, int chargeUp,
                        int duration, int cooldown) {
        this.codeName = codeName;
        this.name = name;
        this.origin = origin;
        this.description = description;
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
                Objects.equals(this.activation, that.activation) &&
                this.chargeUp == that.chargeUp &&
                this.duration == that.duration &&
                this.cooldown == that.cooldown;
    }

    @Override
    public int hashCode() {
        return Objects.hash(codeName, name, origin, description, activation, chargeUp, duration, cooldown);
    }

    @Override
    public String toString() {
        return "AbilityInfo[" +
                "codeName=" + codeName + ", " +
                "name=" + name + ", " +
                "origin=" + origin + ", " +
                "description=" + description + ", " +
                "activation=" + activation + ", " +
                "chargeUp=" + chargeUp + ", " +
                "duration=" + duration + ", " +
                "cooldown=" + cooldown + ']';
    }

    public static class Builder {
        private String codeName;
        private String name;
        private String origin;
        private String description;
        private ActivationHand activation;
        private int chargeUp;
        private int duration;
        private int cooldown;

        public Builder() {
        }

        public Builder(AbilityInfo info) {
            codeName = info.codeName;
            name = info.name;
            origin = info.origin;
            description = info.description;
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
            return new AbilityInfo(codeName, name, origin, description, activation, chargeUp, duration, cooldown);
        }
    }
}
