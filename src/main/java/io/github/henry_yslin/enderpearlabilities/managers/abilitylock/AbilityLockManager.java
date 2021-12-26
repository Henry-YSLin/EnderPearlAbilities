package io.github.henry_yslin.enderpearlabilities.managers.abilitylock;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.managers.Manager;
import io.github.henry_yslin.enderpearlabilities.utils.EntityUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

@Instantiable
public class AbilityLockManager extends Manager {

    private static AbilityLockManager instance = null;

    public static AbilityLockManager getInstance() {
        return instance;
    }

    @Override
    public String getCodeName() {
        return "ability-lock";
    }

    public AbilityLockManager(Plugin plugin) {
        super(plugin);

        if (instance != null)
            throw new RuntimeException("AbilityLockManager already exists!");
        instance = this;
    }

    @Override
    protected void readFromConfig(ConfigurationSection config) {
        // no configs yet
    }

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        // no configs yet
    }

    public boolean isAbilityLocked(Player player) {
        Optional<Object> lock = EntityUtils.getMetadata(player, "ability-lock");
        return lock.filter(o -> (boolean) o).isPresent();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            unlockAbility(player);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    public void lockAbility(Player player) {
        if (isAbilityLocked(player)) return;

        player.setMetadata("ability-lock", new FixedMetadataValue(plugin, true));
    }

    public void unlockAbility(Player player) {
        if (!isAbilityLocked(player)) return;

        player.removeMetadata("ability-lock", plugin);
    }
}

