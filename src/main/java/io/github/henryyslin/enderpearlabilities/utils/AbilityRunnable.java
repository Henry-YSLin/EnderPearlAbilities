package io.github.henryyslin.enderpearlabilities.utils;

import io.github.henryyslin.enderpearlabilities.Ability;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public abstract class AbilityRunnable extends BukkitRunnable {
    protected long count = Long.MIN_VALUE;
    protected Ability ability;

    public boolean hasCompleted() {
        return count < 0;
    }

    private void internalStart(Ability ability) {
        this.ability = ability;
        ability.runnables.add(this);
        start();
    }

    private void internalEnd() {
        ability.runnables.remove(this);
        end();
    }

    protected void start() {
    }

    protected void tick() {
    }

    protected void end() {
    }

    @Override
    public final synchronized void run() {
        tick();

        if (count == Long.MIN_VALUE) return;
        count--;
        if (hasCompleted()) {
            cancel();
        }
    }

    @Override
    @Deprecated
    public final synchronized @NotNull BukkitTask runTaskLater(@NotNull Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
        return super.runTaskLater(plugin, delay);
    }

    @Override
    @Deprecated
    public final synchronized @NotNull BukkitTask runTaskTimer(@NotNull Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
        return super.runTaskTimer(plugin, delay, period);
    }

    public final synchronized BukkitTask runTaskRepeated(Ability ability, long delay, long period, long repeat) throws IllegalArgumentException, IllegalStateException {
        count = repeat - 1;
        return runTaskTimer(ability, delay, period);
    }

    public final synchronized BukkitTask runTaskTimer(Ability ability, long delay, long period) throws IllegalArgumentException, IllegalStateException {
        internalStart(ability);
        return super.runTaskTimer(ability.plugin, delay, period);
    }

    public final synchronized BukkitTask runTaskLater(Ability ability, long delay) throws IllegalArgumentException, IllegalStateException {
        count = 0;
        internalStart(ability);
        return super.runTaskLater(ability.plugin, delay);
    }

    @Override
    public final synchronized void cancel() throws IllegalStateException {
        internalEnd();
        if (!isCancelled())
            super.cancel();
    }
}
