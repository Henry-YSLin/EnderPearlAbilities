package io.github.henryyslin.enderpearlabilities.utils;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public abstract class AdvancedRunnable extends BukkitRunnable {
    protected long count = Long.MIN_VALUE;

    protected void start() {}
    protected void tick() {}
    protected void end() {}

    @Override
    public synchronized void run() {
        tick();

        if (count == Long.MIN_VALUE) return;
        count--;
        if (count <= 0) {
            cancel();
        }
    }

    public synchronized BukkitTask runTaskRepeated(Plugin plugin, long delay, long period, long repeat) throws IllegalArgumentException, IllegalStateException {
        count = repeat - 1;
        return runTaskTimer(plugin, delay, period);
    }

    @Override
    public synchronized BukkitTask runTaskTimer(Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
        start();
        return super.runTaskTimer(plugin, delay, period);
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        end();
        super.cancel();
    }
}
