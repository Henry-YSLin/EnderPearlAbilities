package io.github.henryyslin.enderpearlabilities.necromancer;

import io.github.henryyslin.enderpearlabilities.utils.AdvancedRunnable;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.atomic.AtomicReference;

public class SlaveSpawning extends AdvancedRunnable {
    Plugin plugin;
    Player player;
    Skeleton skeleton;
    AtomicReference<LivingEntity> playerTarget;
    Location particleLocation;

    public SlaveSpawning(Plugin plugin, Player player, Skeleton skeleton, AtomicReference<LivingEntity> playerTarget) {
        this.plugin = plugin;
        this.player = player;
        this.skeleton = skeleton;
        this.playerTarget = playerTarget;
    }

    @Override
    protected void start() {
        this.particleLocation = skeleton.getLocation().add(0, 1, 0);
    }

    @Override
    protected void tick() {
        if (!skeleton.isValid()) {
            cancel();
            return;
        }
        skeleton.teleport(skeleton.getLocation().add(0, 0.1, 0));
        skeleton.getWorld().spawnParticle(Particle.SQUID_INK, particleLocation, 2, 0.5, 1, 0.5, 0.02);
    }

    @Override
    protected void end() {
        if (!skeleton.isValid()) return;
        skeleton.setAI(true);
        new SlaveAI(player, skeleton, playerTarget).runTaskTimer(plugin, 0, 10);
    }
}
