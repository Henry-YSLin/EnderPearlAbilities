package io.github.henryyslin.enderpearlabilities;

import io.github.henryyslin.enderpearlabilities.abilities.Ability;
import io.github.henryyslin.enderpearlabilities.commands.ability.AbilityCommand;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.reflections.scanners.Scanners.SubTypes;

public final class EnderPearlAbilities extends JavaPlugin {

    final FileConfiguration config = getConfig();
    final List<Ability> internalTemplateAbilities = new ArrayList<>();
    final List<Ability> templateAbilities = Collections.unmodifiableList(internalTemplateAbilities);
    final List<Ability> abilities = Collections.synchronizedList(new ArrayList<>());
    static EnderPearlAbilities instance;

    public static EnderPearlAbilities getInstance() {
        return instance;
    }

    public List<Ability> getTemplateAbilities() {
        return templateAbilities;
    }

    public List<Ability> getAbilities() {
        return abilities;
    }

    public FileConfiguration getLoadedConfig() {
        return config;
    }

    @Override
    public void onEnable() {
        instance = this;

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
                Ability template = ability.getClass().getDeclaredConstructor(Plugin.class, String.class, ConfigurationSection.class).newInstance(this, null, section);
                internalTemplateAbilities.add(template);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        getLogger().info("Saving config");

        config.addDefault("no-cooldown", false);
        config.addDefault("dynamic", false);
        config.options().copyDefaults(true);
        saveConfig();

        getLogger().info("Setting up abilities");

        PluginCommand command = this.getCommand("ability");
        if (command != null) {
            command.setExecutor(new AbilityCommand());
        } else {
            getServer().broadcastMessage("Failed to register /ability command. Please check plugin description file.");
        }

        if (config.getBoolean("no-cooldown")) {
            getLogger().info("No cooldown mode is ON. All ability cooldowns are set to 1s.");
        }

        for (Ability templateAbility : templateAbilities) {
            try {
                String codeName = templateAbility.getInfo().codeName;
                ConfigurationSection configSection = config.getConfigurationSection(codeName);
                if (configSection == null) configSection = config.createSection(codeName);
                List<String> players = configSection.getStringList("players");
                for (String player : players) {
                    Ability instance = templateAbility.getClass().getDeclaredConstructor(Plugin.class, String.class, ConfigurationSection.class).newInstance(this, player, configSection);
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
                synchronized (abilities) {
                    for (Ability ability : abilities) {
                        getLogger().info("Calling onEnable for \"" + ability.getInfo().codeName + "\"");
                        ability.onEnable();
                    }
                }
            }
        }.runTaskLater(this, 1);

        getLogger().info("Plugin onEnable completed");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        synchronized (abilities) {
            for (Ability ability : abilities) {
                getLogger().info("Calling onDisable for \"" + ability.getInfo().codeName + "\"");
                ability.onDisable();
            }
        }
        getLogger().info("Plugin onDisable completed");
    }

    public void removeAbility(Ability ability) {
        synchronized (abilities) {
            if (!abilities.contains(ability)) {
                return;
            }
            abilities.remove(ability);
            HandlerList.unregisterAll(ability);
            ability.onDisable();
            getLogger().info("Calling onDisable for \"" + ability.getInfo().codeName + "\"");
        }
    }

    public Ability addAbility(Ability templateAbility, String ownerName) {
        try {
            String codeName = templateAbility.getInfo().codeName;
            ConfigurationSection configSection = config.getConfigurationSection(codeName);
            if (configSection == null) configSection = config.createSection(codeName);
            Ability instance = templateAbility.getClass().getDeclaredConstructor(Plugin.class, String.class, ConfigurationSection.class).newInstance(this, ownerName, configSection);
            abilities.add(instance);
            getServer().getPluginManager().registerEvents(instance, this);
            getLogger().info("Setting up \"" + codeName + "\" for " + ownerName);
            instance.onEnable();
            return instance;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
