package io.github.henry_yslin.enderpearlabilities.abilities.bangaloretactical;

import io.github.henry_yslin.enderpearlabilities.BasicExtendedListener;
import io.github.henry_yslin.enderpearlabilities.BasicExtendedRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class DoubleTimeListener extends BasicExtendedListener {

    static final int SPEED_DURATION = 40;
    static final double DETECT_RANGE = 2;

    private final Ability ability;

    public DoubleTimeListener(Plugin plugin, Ability ability, @Nullable ConfigurationSection config) {
        super(plugin, config);
        this.ability = ability;
    }

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        super.setConfigDefaults(config);
    }

    BasicExtendedRunnable projectileRunnable;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getName().equals(ability.ownerName)) {
            setUpPlayer(player);
        }
    }

    @Override
    public void onEnable() {
        if (ability.player != null) {
            setUpPlayer(ability.player);
        }
    }

    private void setUpPlayer(Player player) {
        if (projectileRunnable != null && !projectileRunnable.isCancelled())
            projectileRunnable.cancel();
        (projectileRunnable = new BasicExtendedRunnable() {
            @Override
            protected void tick() {
                super.tick();
                if (!player.isValid()) return;
                if (player.getWorld()
                        .getNearbyEntities(player.getEyeLocation(), DETECT_RANGE, DETECT_RANGE, DETECT_RANGE,
                                entity -> entity instanceof Projectile projectile && !projectile.isOnGround() && !Objects.equals(projectile.getShooter(), player)
                        ).size() > 0
                ) {
                    if (player.isSprinting())
                        player.addPotionEffect(PotionEffectType.SPEED.createEffect(SPEED_DURATION, 1));
                }
            }
        }).runTaskTimer(this, 0, 1);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.getName().equals(ability.ownerName)) return;

        if (player.isSprinting())
            player.addPotionEffect(PotionEffectType.SPEED.createEffect(SPEED_DURATION, 1));
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
