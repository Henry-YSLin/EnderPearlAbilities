package io.github.henryyslin.enderpearlabilities;

import io.github.henryyslin.enderpearlabilities.utils.AbilityRunnable;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;

// TODO: multiple cooldowns on the same player

public class AbilityCooldown {
    final Ability ability;
    final FileConfiguration config;
    final Player player;
    final AtomicBoolean coolingDown = new AtomicBoolean(false);
    AbilityCooldownRunnable runnable;

    public boolean getCoolingDown() {
        return coolingDown.get();
    }

    public AbilityCooldown(Ability ability, Player player) {
        this.ability = ability;
        this.config = ability.plugin.getConfig();
        this.player = player;
    }

    public void startCooldown(int ticks) {
        if (ability.plugin.getConfig().getBoolean("no-cooldown"))
            ticks = 20;
        player.setCooldown(Material.ENDER_PEARL, ticks);
        runnable = new AbilityCooldownRunnable();
        runnable.runTaskRepeated(ability, 0, 1, ticks);
    }

    public void cancelCooldown() {
        if (runnable != null) {
            player.setCooldown(Material.ENDER_PEARL, 0);
            runnable.cancel();
        }
    }

    class AbilityCooldownRunnable extends AbilityRunnable {
        @Override
        protected void start() {
            coolingDown.set(true);
        }

        @Override
        protected void tick() {
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation(), 2, 0.5, 0.5, 0.5, 0.02);
        }

        @Override
        protected void end() {
            coolingDown.set(false);
        }
    }
}
