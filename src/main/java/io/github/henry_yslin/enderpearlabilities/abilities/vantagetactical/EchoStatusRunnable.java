package io.github.henry_yslin.enderpearlabilities.abilities.vantagetactical;

import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class EchoStatusRunnable extends AbilityRunnable {

    final VantageTacticalAbility ability;
    final Player player;
    final AtomicReference<LivingEntity> echo;
    final AtomicBoolean chargingUp;
    final AtomicBoolean abilityActive;

    public EchoStatusRunnable(VantageTacticalAbility ability, Player player, AtomicReference<LivingEntity> echo, AtomicBoolean chargingUp, AtomicBoolean abilityActive) {
        this.ability = ability;
        this.player = player;
        this.echo = echo;
        this.chargingUp = chargingUp;
        this.abilityActive = abilityActive;
    }

    @Override
    public void tick() {
        LivingEntity bat = echo.get();
        // manage echo location
        if (bat != null && bat.isValid() && !ability.isMoving()) {
            double distance = bat.getLocation().distance(player.getLocation());
            if (distance > VantageTacticalAbility.MAX_RANGE + 15) {
                bat.remove();
                echo.set(null);
            } else if (distance > VantageTacticalAbility.MAX_RANGE + 5) {
                Vector targetLocation = player.getLocation().toVector().add(bat.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0).normalize().multiply(VantageTacticalAbility.MAX_RANGE)).add(new Vector(0, VantageTacticalAbility.ECHO_ELEVATION, 0));
                Vector offset = targetLocation.subtract(bat.getLocation().toVector()).normalize().multiply(VantageTacticalAbility.FLY_SPEED);
                bat.setVelocity(offset);
            } else {
                bat.setVelocity(new Vector(0, 0, 0));
            }
        }
        // update hints on screen
        if (ability.getCooldown().isCoolingDown()) return;
        if (chargingUp.get()) return;
        if (abilityActive.get()) return;
        boolean mainHandPearl = player.getInventory().getItemInMainHand().getType() == Material.ENDER_PEARL;
        boolean offHandPearl = player.getInventory().getItemInOffHand().getType() == Material.ENDER_PEARL;
        if (ability.getInfo().getActivation() == ActivationHand.MainHand && mainHandPearl ||
                ability.getInfo().getActivation() == ActivationHand.OffHand && offHandPearl) {
            if (bat != null && bat.isValid()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.LIGHT_PURPLE + "Ready to launch, sneak to relocate"));
            } else {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.LIGHT_PURPLE + "Deploy Echo"));
            }
        }
    }
}
