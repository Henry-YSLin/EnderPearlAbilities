package io.github.henry_yslin.enderpearlabilities.abilities.bangaloretactical;

import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityListener;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;

public class DoubleTimeListener extends AbilityListener {

    static final int SPEED_DURATION = 40;
    static final double DETECT_RANGE = 2;

    protected final Ability ability;

    public DoubleTimeListener(Plugin plugin, Ability ability) {
        super(plugin);
        this.ability = ability;
    }

    AbilityRunnable projectileRunnable;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getName().equals(ability.getOwnerName())) {
            setUpPlayer(player);
        }
    }

    @Override
    public void onEnable() {
        if (ability.getPlayer() != null) {
            setUpPlayer(ability.getPlayer());
        }
    }

    private void setUpPlayer(Player player) {
        if (projectileRunnable != null && !projectileRunnable.isCancelled())
            projectileRunnable.cancel();
        (projectileRunnable = new AbilityRunnable() {
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
        if (!player.getName().equals(ability.getOwnerName())) return;

        if (player.isSprinting())
            player.addPotionEffect(PotionEffectType.SPEED.createEffect(SPEED_DURATION, 1));
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
