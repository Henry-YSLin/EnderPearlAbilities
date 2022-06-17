package io.github.henry_yslin.enderpearlabilities.abilities.thunderstrike;

import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class RainstormRunnable extends AbilityRunnable {

    final Player player;

    public RainstormRunnable(Player player) {
        this.player = player;
    }

    @Override
    public void tick() {
        if (!player.getWorld().isClearWeather()) {
            player.addPotionEffect(PotionEffectType.SPEED.createEffect(15, 0));
        }
    }
}
