package io.github.henry_yslin.enderpearlabilities.utils;

import io.github.henry_yslin.enderpearlabilities.abilities.*;
import io.github.henry_yslin.enderpearlabilities.managers.Manager;
import io.github.henry_yslin.enderpearlabilities.managers.ManagerRunnable;
import io.github.henry_yslin.enderpearlabilities.managers.interactionlock.InteractionLockManager;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Consumer;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class AbilityUtils {

    /**
     * Display a standard charge-up effect to the player for a specified amount of time, then run the {@code next} runnable.
     *
     * @param ability  The {@link Ability} to register the charge-up runnable to.
     * @param player   The player to display the charge-up effect to.
     * @param chargeUp The duration of charge-up in ticks.
     * @param next     The runnable to run when the charge-up is complete.
     */
    public static void chargeUpSequence(Ability<?> ability, Player player, int chargeUp, AtomicBoolean chargingUp, Runnable next) {
        chargeUpSequence(ability, player, chargeUp, chargingUp, next, null);
    }

    /**
     * Display a standard charge-up effect to the player for a specified amount of time, then run the {@code next} runnable.
     *
     * @param ability    The {@link Ability} to register the charge-up runnable to.
     * @param player     The player to display the charge-up effect to.
     * @param chargeUp   The duration of charge-up in ticks.
     * @param chargingUp The flag for an in-progress charge-up sequence.
     * @param next       The runnable to run when the charge-up is complete.
     * @param onTick     Additional actions on charge up tick, given {@code count} as argument.
     */
    public static void chargeUpSequence(Ability<?> ability, Player player, int chargeUp, AtomicBoolean chargingUp, Runnable next, @Nullable Consumer<Long> onTick) {
        if (chargeUp <= 0) {
            next.run();
            return;
        }
        chargingUp.set(true);
        new AbilityRunnable() {
            BossBar bossbar;

            @Override
            protected synchronized void start() {
                // player.setCooldown(Material.ENDER_PEARL, chargeUp);
                bossbar = Bukkit.createBossBar("Charging up", BarColor.WHITE, BarStyle.SOLID);
                bossbar.addPlayer(player);
            }

            @Override
            protected synchronized void tick() {
                if (!player.isValid()) {
                    cancel();
                    return;
                }
                bossbar.setProgress(count / (double) chargeUp);
                player.getWorld().spawnParticle(Particle.WHITE_ASH, player.getLocation(), 5, 0.5, 0.5, 0.5, 0.02);
                if (onTick != null) onTick.accept(count);
            }

            @Override
            protected synchronized void end() {
                bossbar.removeAll();
                chargingUp.set(false);
                if (this.hasCompleted())
                    next.run();
            }
        }.runTaskRepeated(ability, 0, 1, chargeUp);
    }

    /**
     * Remove an ender pearl from the player's inventory if necessary.
     * <p>
     * Only main hand abilities will remove the ender pearl.
     * <p>
     * See {@link PlayerUtils#consumeEnderPearl(Player)} for more information.
     *
     * @param ability The {@link Ability} to remove an ender pearl for.
     * @param player  The player to remove an ender pearl from.
     * @return Whether the ender pearl is successfully removed.
     */
    public static boolean consumeEnderPearl(Ability<?> ability, Player player) {
        if (ability.getInfo().getActivation() == ActivationHand.MainHand)
            return PlayerUtils.consumeEnderPearl(player);
        else
            return true;
    }

    /**
     * Check whether a given {@link PlayerInteractEvent} should activate an ability.
     *
     * @param event          The related {@link PlayerInteractEvent}.
     * @param ownerName      The name of the player that possesses the ability.
     * @param activationHand The activation hand of the ability.
     * @return Whether the ability should activate.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean abilityShouldActivate(PlayerInteractEvent event, String ownerName, ActivationHand activationHand) {
        return abilityShouldActivate(event, ownerName, activationHand, false);
    }

    /**
     * Check whether a given {@link PlayerInteractEvent} should activate an ability.
     *
     * @param event                 The related {@link PlayerInteractEvent}.
     * @param ownerName             The name of the player that possesses the ability.
     * @param activationHand        The activation hand of the ability.
     * @param ignoreInteractionLock Whether to still return true if interaction is locked.
     * @return Whether the ability should activate.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean abilityShouldActivate(PlayerInteractEvent event, String ownerName, ActivationHand activationHand, boolean ignoreInteractionLock) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack item = event.getItem();

        if (!ignoreInteractionLock && InteractionLockManager.getInstance().isInteractionLocked(player)) return false;
        if (!player.getName().equals(ownerName)) return false;
        if (activationHand == ActivationHand.MainHand) {
            if (player.getInventory().getItemInMainHand().getType() != Material.ENDER_PEARL) return false;
        } else {
            if (player.getInventory().getItemInOffHand().getType() != Material.ENDER_PEARL) return false;
        }
        if (item == null || item.getType() != Material.ENDER_PEARL) return false;
        if (player.getCooldown(Material.ENDER_PEARL) > 0) return false;
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }

    /**
     * Make a player fire a projectile as if they have thrown it themselves.
     *
     * @param ability            The ability responsible for this projectile.
     * @param player             The player to fire this projectile from.
     * @param inFlight           An {@link AtomicBoolean} to track whether the projectile is still in flight.
     * @param projectileLifetime The maximum flight duration allowed (in ticks) before the projectile is killed.
     * @param projectileSpeed    The initial speed of the projectile.
     * @param gravity            Whether the projectile is affected by gravity.
     * @return The projectile that is fired.
     */
    public static Projectile fireProjectile(Ability<?> ability, Player player, AtomicBoolean inFlight, int projectileLifetime, double projectileSpeed, boolean gravity) {
        if (inFlight != null) {
            if (inFlight.get()) return null;
            inFlight.set(true);
        }

        Projectile projectile = player.launchProjectile(Snowball.class, player.getLocation().getDirection().normalize().multiply(projectileSpeed));
        projectile.setGravity(gravity);

        projectile.setMetadata("ability", new FixedMetadataValue(ability.getPlugin(), new AbilityCouple(ability.getInfo().getCodeName(), player.getName())));

        player.setCooldown(Material.ENDER_PEARL, 1);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 1, 0);

        delay(ability, projectileLifetime, () -> {
            if (projectile.isValid()) {
                projectile.remove();
            }
            if (inFlight != null)
                inFlight.set(false);
        }, true);

        return projectile;
    }

    /**
     * Verify that an {@link AbilityCouple} belongs to the given {@link Ability} instance.
     *
     * @param ability The {@link Ability} instance.
     * @param couple  The {@link AbilityCouple}.
     * @return Whether the {@link AbilityCouple} belongs to the given {@link Ability}.
     */
    public static boolean verifyAbilityCouple(Ability<?> ability, AbilityCouple couple) {
        if (!Objects.equals(ability.getInfo().getCodeName(), couple.ability())) return false;
        return Objects.equals(ability.getOwnerName(), couple.player());
    }

    /**
     * Verify that an entity belongs to the given {@link Ability} instance, by verifying the
     * {@link AbilityCouple} stored in the entity's metadata.
     *
     * @param ability The {@link Ability} instance.
     * @param entity  The target entity.
     * @return Whether the entity belongs to the given {@link Ability} instance.
     */
    public static boolean verifyAbilityCouple(Ability<?> ability, Entity entity) {
        Optional<Object> couple = EntityUtils.getMetadata(entity, "ability");
        if (couple.isEmpty()) return false;
        if (!(couple.get() instanceof AbilityCouple abilityCouple)) return false;
        return verifyAbilityCouple(ability, abilityCouple);
    }

    /**
     * Execute a runnable after some delay.
     *
     * @param ability        The ability that owns the runnable.
     * @param delay          The delay in ticks.
     * @param next           The runnable to execute after the delay.
     * @param runIfCancelled Whether to execute the runnable immediately if the delay is cancelled.
     */
    public static void delay(Ability<?> ability, int delay, Runnable next, boolean runIfCancelled) {
        new AbilityRunnable() {
            @Override
            protected void tick() {
                if (!runIfCancelled) next.run();
            }

            @Override
            protected void end() {
                if (runIfCancelled) next.run();
            }
        }.runTaskLater(ability, delay);
    }

    /**
     * Execute a runnable after some delay.
     *
     * @param manager        The manager that owns the runnable.
     * @param delay          The delay in ticks.
     * @param next           The runnable to execute after the delay.
     * @param runIfCancelled Whether to execute the runnable immediately if the delay is cancelled.
     */
    public static void delay(Manager manager, int delay, Runnable next, boolean runIfCancelled) {
        new ManagerRunnable() {
            @Override
            protected void tick() {
                if (!runIfCancelled) next.run();
            }

            @Override
            protected void end() {
                if (runIfCancelled) next.run();
            }
        }.runTaskLater(manager, delay);
    }

    private static String friendlyNumber(int number) {
        if (number == Integer.MAX_VALUE) return "infinite";
        return String.valueOf(number / 20f);
    }

    /**
     * Generate a formatted description of an ability.
     *
     * @param abilityInfo The ability to describe.
     * @return A description of the ability, formatted with special format characters.
     */
    public static String formatAbilityInfo(AbilityInfo abilityInfo, @Nullable String ownerName, boolean showUsage) {
        String s;
        if (ownerName != null)
            s = String.format("%s - %s%s%s - %s", ownerName, ChatColor.LIGHT_PURPLE, ChatColor.BOLD, abilityInfo.getOrigin(), abilityInfo.getName());
        else
            s = String.format("%s%s%s - %s", ChatColor.LIGHT_PURPLE, ChatColor.BOLD, abilityInfo.getOrigin(), abilityInfo.getName());
        s += "\n" +
                ChatColor.RESET + ChatColor.WHITE + abilityInfo.getDescription() +
                "\n";
        if (showUsage)
            s += ChatColor.BOLD + "Usage: " + ChatColor.RESET + abilityInfo.getUsage() +
                    "\n";
        s += String.format("%sActivation: %s", ChatColor.GRAY, abilityInfo.getActivation() == ActivationHand.MainHand ? "main hand" : "off hand") +
                "\n" +
                String.format("%sCharge up: %ss   Duration: %ss   Cool down: %ss", ChatColor.GRAY, friendlyNumber(abilityInfo.getChargeUp()), friendlyNumber(abilityInfo.getDuration()), friendlyNumber(abilityInfo.getCooldown())) +
                "\n";
        return s;
    }

    public static String formatAbilityInfo(Ability<?> ability, boolean showUsage) {
        return formatAbilityInfo(ability.getInfo(), ability.getOwnerName(), showUsage);
    }

    public static String formatAbilityInfo(AbilityInfo abilityInfo, boolean showUsage) {
        return formatAbilityInfo(abilityInfo, null, showUsage);
    }
}
