package io.github.henryyslin.enderpearlabilities;

import io.github.henryyslin.enderpearlabilities.pathfinder.AbilityPathfinder;
import io.github.henryyslin.enderpearlabilities.wraith.AbilityWraith;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class EnderPearlAbilities extends JavaPlugin {
    final FileConfiguration config = getConfig();
    final Ability[] abilities = {
        new AbilityWraith(this, config),
        new AbilityPathfinder(this, config)
    };

    @Override
    public void onEnable() {
        // Plugin startup logic
        for (Ability ability: abilities) {
            config.addDefault(ability.getConfigName(), "null");
        }
        config.options().copyDefaults(true);
        saveConfig();

        getLogger().info("Setting up abilities");

        this.getCommand("ability").setExecutor(new CommandAbility(config, abilities));

        for (Ability ability: abilities) {
            getServer().getPluginManager().registerEvents(ability, this);
            getLogger().info("Setting up \"" + ability.getName() + "\" for " + config.getString(ability.getConfigName()));
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("onDisable is called!");
    }
}
