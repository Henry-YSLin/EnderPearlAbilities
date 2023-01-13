package io.github.henry_yslin.enderpearlabilities.abilities;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the timing, effects and UI of an ability cooldown.
 * This cooldown allows an ability to have multiple charges.
 */
public class MultipleChargeCooldown implements AbilityCooldown {

    final Ability<?> ability;
    final FileConfiguration config;
    final Player player;
    final int maxCharge;
    final boolean visible;
    final ConcurrentLinkedQueue<AtomicInteger> charges = new ConcurrentLinkedQueue<>();
    MultipleChargeCooldownRunnable runnable;

    /**
     * Get whether the ability can be used at the moment.
     *
     * @return Whether the ability can be used at the moment.
     */
    public boolean isAbilityUsable() {
        return charges.size() < maxCharge;
    }

    /**
     * Get the number of charges available to be used.
     *
     * @return The number of charges available to be used.
     */
    public int getAvailableCharge() {
        return maxCharge - charges.size();
    }

    /**
     * Get the remaining ticks until another use of the ability is allowed.
     *
     * @return The remaining ticks.
     */
    public int getCooldownTicks() {
        if (isAbilityUsable()) return 0;
        AtomicInteger cd = charges.peek();
        return cd == null ? 0 : cd.get();
    }

    public MultipleChargeCooldown(Ability<?> ability, Player player, int maxCharge, boolean visible) {
        this.ability = ability;
        this.config = ability.getPlugin().getConfig();
        this.player = player;
        this.maxCharge = maxCharge;
        this.visible = visible;
    }

    public MultipleChargeCooldown(Ability<?> ability, Player player, int maxCharge) {
        this(ability, player, maxCharge, true);
    }

    /**
     * Cancel the current cooldown, allowing ability use immediately.
     */
    public void cancelCooldown() {
        charges.poll();
        if (charges.size() == 0) {
            if (runnable != null)
                runnable.cancel();
        }
    }

    /**
     * Set the number of ticks until the next use of the ability is allowed.
     *
     * @param ticks The new cooldown value.
     */
    public void setCooldown(int ticks) {
        if (ability.getPlugin().getConfig().getBoolean("no-cooldown"))
            ticks = Math.min(20, ticks);
        if (ticks > 0 && charges.size() < maxCharge)
            charges.offer(new AtomicInteger(ticks));
        if (charges.size() > 0) {
            if (runnable == null || runnable.isCancelled()) {
                runnable = new MultipleChargeCooldownRunnable();
                runnable.runTaskTimer(ability, 0, 1);
            }
        } else {
            cancelCooldown();
        }
    }

    class MultipleChargeCooldownRunnable extends AbilityRunnable {
        @Override
        protected void start() {
        }

        @Override
        protected void tick() {
            AtomicInteger boxedTicks = charges.peek();
            int ticks = boxedTicks == null ? 0 : boxedTicks.getAndDecrement();
            if (ticks <= 0) {
                charges.poll();
            }
            if (charges.size() == 0) {
                cancel();
                return;
            }
            if (!visible) return;
            boolean mainHandPearl = player.getInventory().getItemInMainHand().getType() == Material.ENDER_PEARL;
            boolean offHandPearl = player.getInventory().getItemInOffHand().getType() == Material.ENDER_PEARL;
            if (ability.getInfo().getActivation() == ActivationHand.MainHand && mainHandPearl ||
                    ability.getInfo().getActivation() == ActivationHand.OffHand && offHandPearl) {
                if (isAbilityUsable()) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent((maxCharge - charges.size()) + "x " + ability.getInfo().getName() + " (" + ticks / 20 + "s)"));
                } else {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ability.getInfo().getName() + " in " + ticks / 20 + "s"));
                }
            } else if (!mainHandPearl && !offHandPearl)
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(" "));
            player.getWorld().spawnParticle(Particle.SUSPENDED, player.getLocation(), 1, 0.5, 0.5, 0.5, 0.02);
        }

        @Override
        protected void end() {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(maxCharge + "x " + ability.getInfo().getName()));
        }
    }
}
