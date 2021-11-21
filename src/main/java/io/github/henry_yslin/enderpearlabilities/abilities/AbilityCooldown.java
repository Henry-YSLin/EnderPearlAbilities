package io.github.henry_yslin.enderpearlabilities.abilities;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the timing, effects and UI of an ability cooldown.
 */
public class AbilityCooldown {

    final Ability ability;
    final FileConfiguration config;
    final Player player;
    final boolean visible;
    final AtomicInteger cooldownTicks = new AtomicInteger(0);
    AbilityCooldownRunnable runnable;

    /**
     * Get whether the cooldown is currently active.
     *
     * @return Whether the cooldown is currently active.
     */
    public boolean getCoolingDown() {
        return cooldownTicks.get() > 0;
    }

    /**
     * Get the remaining ticks until the cooldown ends.
     *
     * @return The remaining ticks until the cooldown ends.
     */
    public int getCooldownTicks() {
        return cooldownTicks.get();
    }

    public AbilityCooldown(Ability ability, Player player, boolean visible) {
        this.ability = ability;
        this.config = ability.plugin.getConfig();
        this.player = player;
        this.visible = visible;
    }

    public AbilityCooldown(Ability ability, Player player) {
        this(ability, player, true);
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
        if (runnable != null && !runnable.isCancelled())
            runnable.cancel();
        cooldownTicks.set(ticks);
        runnable = new AbilityCooldownRunnable();
        runnable.runTaskTimer(ability, 0, 1);
    }

    /**
     * Cancel the cooldown, if one is currently active.
     */
    public void cancelCooldown() {
        if (runnable != null) {
            runnable.cancel();
            cooldownTicks.set(0);
        }
    }

    /**
     * Add a specified amount of ticks to the cooldown.
     *
     * @param ticks The number of ticks to add, can be negative.
     * @return The new cooldown duration in ticks.
     */
    public int addCooldown(int ticks) {
        int cd = Math.max(0, cooldownTicks.get() + ticks);
        cooldownTicks.set(cd);
        if (cd > 0) {
            if (runnable == null || runnable.isCancelled()) {
                runnable = new AbilityCooldownRunnable();
                runnable.runTaskTimer(ability, 0, 1);
            }
        }
        return cd;
    }

    class AbilityCooldownRunnable extends AbilityRunnable {
        @Override
        protected void start() {
        }

        @Override
        protected void tick() {
            int count = cooldownTicks.getAndDecrement();
            if (count <= 0)
                cancel();
            if (!visible) return;
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
            cooldownTicks.set(0);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent());
        }
    }
}
