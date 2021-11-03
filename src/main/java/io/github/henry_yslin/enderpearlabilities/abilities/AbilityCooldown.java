package io.github.henry_yslin.enderpearlabilities.abilities;

import io.github.henry_yslin.enderpearlabilities.utils.AbilityRunnable;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the timing, effects and UI of an ability cooldown.
 */
public class AbilityCooldown {

    final Ability ability;
    final FileConfiguration config;
    final Player player;
    final AtomicBoolean coolingDown = new AtomicBoolean(false);
    AbilityCooldownRunnable runnable;

    /**
     * Get whether the cooldown is currently active.
     *
     * @return Whether the cooldown is currently active.
     */
    public boolean getCoolingDown() {
        return coolingDown.get();
    }

    public AbilityCooldown(Ability ability, Player player) {
        this.ability = ability;
        this.config = ability.plugin.getConfig();
        this.player = player;
    }

    /**
     * Start a cooldown sequence of a specified length.
     * The length will be ignored if the {@code no-cooldown} config is set.
     *
     * @param ticks The length of cooldown in ticks.
     */
    public void startCooldown(int ticks) {
        if (ability.plugin.getConfig().getBoolean("no-cooldown"))
            ticks = 20;
        runnable = new AbilityCooldownRunnable();
        runnable.runTaskRepeated(ability, 0, 1, ticks);
    }

    /**
     * Cancel the cooldown, if one is currently active.
     */
    public void cancelCooldown() {
        if (runnable != null) {
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
            boolean mainHandPearl = player.getInventory().getItemInMainHand().getType() == Material.ENDER_PEARL;
            boolean offHandPearl = player.getInventory().getItemInOffHand().getType() == Material.ENDER_PEARL;
            if (ability.getInfo().activation == ActivationHand.MainHand && mainHandPearl ||
                    ability.getInfo().activation == ActivationHand.OffHand && offHandPearl)
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ability.getInfo().name + " in " + count / 20 + "s"));
            else if (!mainHandPearl && !offHandPearl)
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent());
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation(), 2, 0.5, 0.5, 0.5, 0.02);
        }

        @Override
        protected void end() {
            coolingDown.set(false);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent());
        }
    }
}
