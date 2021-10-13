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
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.List;
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
     * Remove an ender pearl from the player's inventory as a result of ability activation.
     * <p>
     * Note: This method does not simply remove an ender pearl from the player's selected hot bar slot.
     * Instead, it first attempts to remove a pearl from the player's backpack, only falling back to the player's
     * main or off hand if there are no pearls in the backpack. This behavior is for the player's convenience,
     * so that they don't need to refill their main/off hands very often.
     *
     * @param player The player to remove an ender pearl from.
     * @return Whether the ender pearl is successfully removed.
     */
    public static boolean consumeEnderPearl(Player player) {
        if (player.getGameMode() != GameMode.CREATIVE) {
            HashMap<Integer, ItemStack> remainingItems = player.getInventory().removeItem(new ItemStack(Material.ENDER_PEARL, 1));
            player.updateInventory();
            if (!remainingItems.isEmpty()) {
                ItemStack heldItem = player.getInventory().getItemInMainHand();
                if (heldItem.getType() == Material.ENDER_PEARL) {
                    heldItem.setAmount(heldItem.getAmount() - 1);
                    if (heldItem.getAmount() == 0) {
                        heldItem.setType(Material.AIR);
                    }
                    player.getInventory().setItemInMainHand(heldItem);
                } else {
                    heldItem = player.getInventory().getItemInOffHand();
                    if (heldItem.getType() == Material.ENDER_PEARL) {
                        heldItem.setAmount(heldItem.getAmount() - 1);
                        if (heldItem.getAmount() == 0) {
                            heldItem.setType(Material.AIR);
                        }
                        player.getInventory().setItemInOffHand(heldItem);
                    } else {
                        return false;
                    }
                }
            }
        }
        return true;
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

        consumeEnderPearl(player);

        Projectile projectile = player.launchProjectile(EnderPearl.class, player.getLocation().getDirection().clone().normalize().multiply(projectileSpeed));
        projectile.setGravity(gravity);

        projectile.setMetadata("ability", new FixedMetadataValue(ability.plugin, new AbilityCouple(ability.getInfo().codeName, player.getName())));

        player.setCooldown(Material.ENDER_PEARL, 1);

        new AbilityRunnable() {
            @Override
            public void tick() {
                if (projectile.isValid()) {
                    projectile.remove();
                }
                if (inFlight != null)
                    inFlight.set(false);
            }
        }.runTaskLater(ability, projectileLifetime);

        return projectile;
    }

    /**
     * Improve the accuracy of projectile hit location by ray tracing.
     *
     * @param projectile The projectile to compute hit location for. The hit event should have already fired for this projectile.
     * @return The accurate hit location of the projectile, ray traced from its current velocity.
     */
    public static Location correctProjectileHitLocation(Projectile projectile) {
        RayTraceResult result = projectile.getWorld().rayTrace(projectile.getLocation(), projectile.getVelocity(), projectile.getVelocity().length(), FluidCollisionMode.NEVER, false, 0.1, null);
        if (result == null) {
            return projectile.getLocation();
        }

        return result.getHitPosition().toLocation(projectile.getWorld());
    }

    /**
     * Get the metadata value of a specified key from an entity.
     *
     * @param entity The entity that holds the metadata.
     * @param key    The key for the metadata value.
     * @return The optional metadata value, being empty if the metadata does not exist.
     */
    public static Optional<Object> getMetadata(Entity entity, String key) {
        if (!entity.hasMetadata(key)) return Optional.empty();
        List<MetadataValue> metadata = entity.getMetadata(key);
        if (metadata.size() == 0) return Optional.empty();
        Object value = metadata.get(metadata.size() - 1).value();
        if (value == null) return Optional.empty();
        return Optional.of(value);
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
        Optional<Object> couple = getMetadata(entity, "ability");
        if (couple.isEmpty()) return false;
        return verifyAbilityCouple(ability, (AbilityCouple) couple.get());
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
