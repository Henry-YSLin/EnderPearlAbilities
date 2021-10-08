package io.github.henryyslin.enderpearlabilities;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.reflections.scanners.Scanners.SubTypes;

public final class EnderPearlAbilities extends JavaPlugin {
    final FileConfiguration config = getConfig();
    final List<Ability> abilities = new ArrayList<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        Reflections reflections = new Reflections("io.github.henryyslin.enderpearlabilities");
        Set<Class<?>> subTypes =
                reflections.get(SubTypes.of(Ability.class).asClass());
        for (Class<?> subType : subTypes) {
            try {
                Ability ability = (Ability) subType.getDeclaredConstructor(Plugin.class, String.class, ConfigurationSection.class).newInstance(this, null, null);
                String codeName = ability.getInfo().codeName;
                getLogger().info("Setting config defaults for " + codeName);
                ConfigurationSection section = config.getConfigurationSection(codeName);
                if (section == null) section = config.createSection(codeName);
                ability.setConfigDefaults(section);
                section.addDefault("players", new ArrayList<String>());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        getLogger().info("Saving config");

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

        for (Class<?> subType : subTypes) {
            try {
                Ability ability = (Ability) subType.getDeclaredConstructor(Plugin.class, String.class, ConfigurationSection.class).newInstance(this, null, null);
                String codeName = ability.getInfo().codeName;
                ConfigurationSection configSection = config.getConfigurationSection(codeName);
                if (configSection == null) configSection = config.createSection(codeName);
                List<String> players = configSection.getStringList("players");
                for (String player : players) {
                    Ability instance = (Ability) subType.getDeclaredConstructor(Plugin.class, String.class, ConfigurationSection.class).newInstance(this, player, configSection);
                    abilities.add(instance);
                    getServer().getPluginManager().registerEvents(instance, this);
                    getLogger().info("Setting up \"" + codeName + "\" for " + player);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Ability ability : abilities) {
                    getLogger().info("Calling onEnable for \"" + ability.getInfo().codeName + "\"");
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
            getLogger().info("Calling onDisable for \"" + ability.getInfo().codeName + "\"");
            ability.onDisable();
        }
        getLogger().info("Plugin onDisable completed");
    }
}
