package io.github.henryyslin.enderpearlabilities;

import io.github.henryyslin.enderpearlabilities.horizon.AbilityHorizon;
import io.github.henryyslin.enderpearlabilities.mirage.AbilityMirage;
import io.github.henryyslin.enderpearlabilities.necromancer.AbilityNecromancer;
import io.github.henryyslin.enderpearlabilities.pathfinder.AbilityPathfinder;
import io.github.henryyslin.enderpearlabilities.valkyrie.AbilityValkyrie;
import io.github.henryyslin.enderpearlabilities.worldshaper.AbilityWorldShaper;
import io.github.henryyslin.enderpearlabilities.wraith.AbilityWraith;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class EnderPearlAbilities extends JavaPlugin {
    final FileConfiguration config = getConfig();
    final Ability[] abilities = {
            new AbilityWraith(this, config),
            new AbilityPathfinder(this, config),
            new AbilityMirage(this, config),
            new AbilityValkyrie(this, config),
            new AbilityNecromancer(this, config),
            new AbilityWorldShaper(this, config),
            new AbilityHorizon(this, config),
    };

    @Override
    public void onEnable() {
        // Plugin startup logic
        for (Ability ability : abilities) {
            config.addDefault(ability.getConfigName(), "");
        }
        config.addDefault("no-cooldown", false);
        config.options().copyDefaults(true);
        saveConfig();

        getLogger().info("Setting up abilities");

        PluginCommand command = this.getCommand("ability");
        if (command != null) {
            command.setExecutor(new CommandAbility(config, abilities));
        } else {
            getServer().broadcastMessage("Failed to register /ability command. Please check plugin description file.");
        }

        if (config.getBoolean("no-cooldown")) {
            getLogger().info("No cooldown mode is ON. All ability cooldowns are set to 1s.");
        }

        for (Ability ability : abilities) {
            getServer().getPluginManager().registerEvents(ability, this);
            getLogger().info("Setting up \"" + ability.getName() + "\" for " + config.getString(ability.getConfigName()));
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Ability ability : abilities) {
                    getLogger().info("Calling onEnable for \"" + ability.getName() + "\"");
                    ability.onEnable();
                }
            }
        }.runTaskLater(this, 1);

        getLogger().info("Plugin onEnable completed");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        for (Ability ability : abilities) {
            getLogger().info("Calling onDisable for \"" + ability.getName() + "\"");
            ability.onDisable();
        }
        getLogger().info("Plugin onDisable completed");
    }
}
