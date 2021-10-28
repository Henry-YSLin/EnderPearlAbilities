package io.github.henryyslin.enderpearlabilities.utils;

import io.github.henryyslin.enderpearlabilities.abilities.Ability;
import io.github.henryyslin.enderpearlabilities.abilities.AbilityCouple;
import io.github.henryyslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henryyslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
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
    public static void chargeUpSequence(Ability ability, Player player, int chargeUp, Runnable next) {
        chargeUpSequence(ability, player, chargeUp, next, null);
    }

    /**
     * Display a standard charge-up effect to the player for a specified amount of time, then run the {@code next} runnable.
     *
     * @param ability  The {@link Ability} to register the charge-up runnable to.
     * @param player   The player to display the charge-up effect to.
     * @param chargeUp The duration of charge-up in ticks.
     * @param next     The runnable to run when the charge-up is complete.
     * @param onTick   Additional actions on charge up tick, given {@code count} as argument.
     */
    public static void chargeUpSequence(Ability ability, Player player, int chargeUp, Runnable next, @Nullable Consumer<Long> onTick) {
        if (chargeUp <= 0) {
            next.run();
            return;
        }
        new AbilityRunnable() {
            BossBar bossbar;

            @Override
            protected synchronized void start() {
                player.setCooldown(Material.ENDER_PEARL, chargeUp);
                bossbar = Bukkit.createBossBar("Charging up", BarColor.WHITE, BarStyle.SOLID);
                bossbar.addPlayer(player);
            }

            @Override
            protected synchronized void tick() {
                bossbar.setProgress(count / (double) chargeUp);
                player.getWorld().spawnParticle(Particle.WHITE_ASH, player.getLocation(), 5, 0.5, 0.5, 0.5, 0.02);
                if (onTick != null) onTick.accept(count);
            }

            @Override
            protected synchronized void end() {
                bossbar.removeAll();
                if (this.hasCompleted())
                    next.run();
            }
        }.runTaskRepeated(ability, 0, 1, chargeUp);
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
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack item = event.getItem();

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
     * Make a player fire an ender pearl as if they have thrown it themselves.
     *
     * @param ability            The ability responsible for this ender pearl.
     * @param player             The player to fire this ender pearl from.
     * @param inFlight           An {@link AtomicBoolean} to track whether the ender pearl is still in flight.
     * @param projectileLifetime The maximum flight duration allowed (in ticks) before the ender pearl is killed.
     * @param projectileSpeed    The initial speed of the ender pearl.
     * @param gravity            Whether the ender pearl is affected by gravity.
     * @return The ender pearl that is fired.
     */
    public static Projectile fireEnderPearl(Ability ability, Player player, AtomicBoolean inFlight, int projectileLifetime, double projectileSpeed, boolean gravity) {
        if (inFlight != null) {
            if (inFlight.get()) return null;
            inFlight.set(true);
        }

        PlayerUtils.consumeEnderPearl(player);

        Projectile projectile = player.launchProjectile(EnderPearl.class, player.getLocation().getDirection().clone().normalize().multiply(projectileSpeed));
        projectile.setGravity(gravity);

        projectile.setMetadata("ability", new FixedMetadataValue(ability.plugin, new AbilityCouple(ability.getInfo().codeName, player.getName())));

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
    public static boolean verifyAbilityCouple(Ability ability, AbilityCouple couple) {
        if (!Objects.equals(ability.getInfo().codeName, couple.ability())) return false;
        return Objects.equals(ability.ownerName, couple.player());
    }

    /**
     * Verify that an entity belongs to the given {@link Ability} instance, by verifying the
     * {@link AbilityCouple} stored in the entity's metadata.
     *
     * @param ability The {@link Ability} instance.
     * @param entity  The target entity.
     * @return Whether the entity belongs to the given {@link Ability} instance.
     */
    public static boolean verifyAbilityCouple(Ability ability, Entity entity) {
        Optional<Object> couple = EntityUtils.getMetadata(entity, "ability");
        if (couple.isEmpty()) return false;
        return verifyAbilityCouple(ability, (AbilityCouple) couple.get());
    }

    /**
     * Execute a runnable after some delay.
     *
     * @param ability        The ability that owns the runnable.
     * @param delay          The delay in ticks.
     * @param next           The runnable to execute after the delay.
     * @param runIfCancelled Whether to execute the runnable immediately if the delay is cancelled.
     */
    public static void delay(Ability ability, int delay, Runnable next, boolean runIfCancelled) {
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

    private static String friendlyNumber(int number) {
        if (number == Integer.MAX_VALUE) return "infinite";
        return String.valueOf(number / 20f);
    }

    /**
     * Generate a formatted description of an ability.
     *
     * @param ability The ability to describe.
     * @return A description of the ability, formatted with special format characters.
     */
    public static String formatAbilityInfo(Ability ability, boolean showUsage) {
        AbilityInfo info = ability.getInfo();
        String s;
        if (ability.ownerName != null)
            s = String.format("%s - %s%s%s - %s", ability.ownerName, ChatColor.LIGHT_PURPLE, ChatColor.BOLD, info.origin, info.name);
        else
            s = String.format("%s%s%s - %s", ChatColor.LIGHT_PURPLE, ChatColor.BOLD, info.origin, info.name);
        s += "\n" +
                ChatColor.RESET + ChatColor.WHITE + info.description +
                "\n";
        if (showUsage)
            s += ChatColor.BOLD + "Usage: " + ChatColor.RESET + info.usage +
                    "\n";
        s += String.format("%sActivation: %s", ChatColor.GRAY, info.activation == ActivationHand.MainHand ? "main hand" : "off hand") +
                "\n" +
                String.format("%sCharge up: %ss   Duration: %ss   Cool down: %ss", ChatColor.GRAY, friendlyNumber(info.chargeUp), friendlyNumber(info.duration), friendlyNumber(info.cooldown)) +
                "\n";
        return s;
    }
}
