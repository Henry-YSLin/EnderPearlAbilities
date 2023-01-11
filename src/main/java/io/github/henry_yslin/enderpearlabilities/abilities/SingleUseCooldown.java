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
 * This cooldown allows a single use after a period of cooldown.
 */
public class SingleUseCooldown implements AbilityCooldown {

    final Ability<?> ability;
    final FileConfiguration config;
    final Player player;
    final boolean visible;
    final AtomicInteger cooldownTicks = new AtomicInteger(0);
    SingleUseCooldownRunnable runnable;

    /**
     * Get whether the ability can be used at the moment.
     *
     * @return Whether the ability can be used at the moment.
     */
    public boolean isAbilityUsable() {
        return cooldownTicks.get() <= 0;
    }

    /**
     * Get the remaining ticks until the cooldown ends.
     *
     * @return The remaining ticks until the cooldown ends.
     */
    public int getCooldownTicks() {
        return cooldownTicks.get();
    }

    public SingleUseCooldown(Ability<?> ability, Player player, boolean visible) {
        this.ability = ability;
        this.config = ability.getPlugin().getConfig();
        this.player = player;
        this.visible = visible;
    }

    public SingleUseCooldown(Ability<?> ability, Player player) {
        this(ability, player, true);
    }

    /**
     * Cancel the cooldown, if one is currently active.
     * <p>
     * This is equivalent to calling setCooldown(0).
     */
    public void cancelCooldown() {
        if (runnable != null) {
            runnable.cancel();
            cooldownTicks.set(0);
        }
    }

    /**
     * Set current cooldown to the specified value. The cooldown sequence may start/stop depending on the new value.
     *
     * @param ticks The new cooldown value.
     */
    public void setCooldown(int ticks) {
        if (ability.getPlugin().getConfig().getBoolean("no-cooldown"))
            ticks = Math.min(20, ticks);
        cooldownTicks.set(ticks);
        if (ticks > 0) {
            if (runnable == null || runnable.isCancelled()) {
                runnable = new SingleUseCooldownRunnable();
                runnable.runTaskTimer(ability, 0, 1);
            }
        } else {
            cancelCooldown();
        }
    }

    class SingleUseCooldownRunnable extends AbilityRunnable {
        @Override
        protected void start() {
        }

        @Override
        protected void tick() {
            int count = cooldownTicks.getAndDecrement();
            if (count <= 0) {
                cancel();
                return;
            }
            if (!visible) return;
            boolean mainHandPearl = player.getInventory().getItemInMainHand().getType() == Material.ENDER_PEARL;
            boolean offHandPearl = player.getInventory().getItemInOffHand().getType() == Material.ENDER_PEARL;
            if (ability.getInfo().getActivation() == ActivationHand.MainHand && mainHandPearl ||
                    ability.getInfo().getActivation() == ActivationHand.OffHand && offHandPearl)
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ability.getInfo().getName() + " in " + count / 20 + "s"));
            else if (!mainHandPearl && !offHandPearl)
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(" "));
            player.getWorld().spawnParticle(Particle.SUSPENDED, player.getLocation(), 1, 0.5, 0.5, 0.5, 0.02);
        }

        @Override
        protected void end() {
            cooldownTicks.set(0);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ability.getInfo().getName() + " ready"));
        }
    }
}
