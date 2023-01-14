package io.github.henry_yslin.enderpearlabilities.abilities.vantageultimate;

import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;

public class MarkedRunnable extends AbilityRunnable {
    BossBar bossBar;
    VantageUltimateAbility ability;
    public final LivingEntity target;
    public final int duration;

    private MarkedRunnable(VantageUltimateAbility ability, LivingEntity target, int duration) {
        this.ability = ability;
        this.target = target;
        this.duration = duration;
    }

    @Override
    protected void start() {
        target.setMetadata("marked", new FixedMetadataValue(ability.getPlugin(), this));
        if (target instanceof Player p) {
            bossBar = Bukkit.createBossBar(ChatColor.RED + "Marked", BarColor.RED, BarStyle.SOLID);
            bossBar.addPlayer(p);
        }
    }

    @Override
    protected void tick() {
        if (bossBar != null) {
            bossBar.setProgress(count * 5d / duration);
        }
        if (!target.isValid()) {
            cancel();
            return;
        }
        target.addPotionEffect(PotionEffectType.GLOWING.createEffect(10, 0));
    }

    @Override
    protected void end() {
        if (bossBar != null) {
            bossBar.removeAll();
        }
        target.removeMetadata("marked", ability.getPlugin());
    }

    public static MarkedRunnable apply(VantageUltimateAbility ability, LivingEntity target, int duration) {
        MarkedRunnable instance = new MarkedRunnable(ability, target, duration);
        instance.runTaskRepeated(ability, 0, 5, duration / 5);
        return instance;
    }
}
