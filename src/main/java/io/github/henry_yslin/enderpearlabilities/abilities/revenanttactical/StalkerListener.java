package io.github.henry_yslin.enderpearlabilities.abilities.revenanttactical;

import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

public class StalkerListener extends AbilityListener {

    protected final Ability<?> ability;

    public StalkerListener(Plugin plugin, Ability<?> ability) {
        super(plugin);
        this.ability = ability;
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!player.getName().equals(ability.getOwnerName())) return;

        if (event.isSneaking()) {
            player.addPotionEffect(PotionEffectType.SPEED.createEffect(Integer.MAX_VALUE, 3));
            player.addPotionEffect(PotionEffectType.JUMP.createEffect(Integer.MAX_VALUE, 1));
        } else {
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.JUMP);
        }
    }
}
