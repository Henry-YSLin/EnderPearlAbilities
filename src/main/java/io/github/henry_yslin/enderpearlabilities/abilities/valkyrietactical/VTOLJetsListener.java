package io.github.henry_yslin.enderpearlabilities.abilities.valkyrietactical;

import io.github.henry_yslin.enderpearlabilities.BasicExtendedListener;
import io.github.henry_yslin.enderpearlabilities.BasicExtendedRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.utils.MathUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class VTOLJetsListener extends BasicExtendedListener {

    static final float FLY_SPEED = 0.04f;

    private final Ability ability;

    public VTOLJetsListener(Plugin plugin, Ability ability, @Nullable ConfigurationSection config) {
        super(plugin, config);
        this.ability = ability;
    }

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        super.setConfigDefaults(config);
    }

    BasicExtendedRunnable vtolJetsRunnable;

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
        player.setAllowFlight(true);

        if (vtolJetsRunnable != null && !vtolJetsRunnable.isCancelled())
            vtolJetsRunnable.cancel();
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (!player.getName().equals(ability.ownerName)) return;
        if (event.isFlying()) {
            if (vtolJetsRunnable != null && !vtolJetsRunnable.isCancelled())
                vtolJetsRunnable.cancel();
            (vtolJetsRunnable = new BasicExtendedRunnable() {
                int stationaryTicks = 0;
                boolean noGravity = false;
                double lastY;

                @Override
                protected void start() {
                    super.start();
                    lastY = player.getLocation().getY();
                }

                @Override
                protected void tick() {
                    if (MathUtils.almostEqual(player.getVelocity().getY(), 0))
                        noGravity = true;
                    if (noGravity && (!MathUtils.almostEqual(player.getVelocity().getY(), 0) || MathUtils.almostSmaller(player.getLocation().getY(), lastY))) {
                        cancel();
                        return;
                    } else {
                        stationaryTicks = 0;
                    }
                    lastY = player.getLocation().getY();
                    player.setFlySpeed(FLY_SPEED);
                }

                @Override
                protected void end() {
                    super.end();
                    player.setFlying(false);
                }
            }).runTaskTimer(this, 0, 1);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
