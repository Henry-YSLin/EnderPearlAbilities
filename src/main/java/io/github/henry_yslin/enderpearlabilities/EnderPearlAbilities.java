package io.github.henry_yslin.enderpearlabilities;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henry_yslin.enderpearlabilities.commands.ability.AbilityCommand;
import io.github.henry_yslin.enderpearlabilities.commands.ability.AbilityTabCompleter;
import io.github.henry_yslin.enderpearlabilities.events.Event;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.managers.Manager;
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
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

import static org.reflections.scanners.Scanners.SubTypes;

public final class EnderPearlAbilities extends JavaPlugin {

    final FileConfiguration config = getConfig();

    final List<AbilityInfo> internalAbilityInfos = new ArrayList<>();
    final List<AbilityInfo> abilityInfos = Collections.unmodifiableList(internalAbilityInfos);
    final List<Ability<?>> abilities = Collections.synchronizedList(new ArrayList<>());

    final List<Manager> internalManagers = new ArrayList<>();
    final List<Manager> managers = Collections.unmodifiableList(internalManagers);

    final List<EventListener> internalEventListeners = new ArrayList<>();
    final List<EventListener> eventListeners = Collections.unmodifiableList(internalEventListeners);

    static EnderPearlAbilities instance;
    static ProtocolManager protocolManager;

    public static EnderPearlAbilities getInstance() {
        return instance;
    }

    public static ProtocolManager getProtocolManager() {
        return protocolManager;
    }

    public List<AbilityInfo> getAbilityInfos() {
        return abilityInfos;
    }

    public List<Ability<?>> getAbilities() {
        return abilities;
    }

    public List<EventListener> getEventListeners() {
        return eventListeners;
    }

    public List<Manager> getManagers() {
        return managers;
    }

    public FileConfiguration getLoadedConfig() {
        return config;
    }

    @Override
    public void onEnable() {
        instance = this;
        protocolManager = ProtocolLibrary.getProtocolManager();
        getLogger().info(protocolManager.toString());

        // Plugin startup logic
        Reflections reflections = new Reflections("io.github.henry_yslin.enderpearlabilities");
        List<Class<?>> managerSubTypes =
                reflections.get(SubTypes.of(Manager.class).asClass()).stream().filter(c -> c.isAnnotationPresent(Instantiable.class)).sorted(Comparator.comparing(Class::getName)).toList();
        for (Class<?> subType : managerSubTypes) {
            try {
                Manager manager = (Manager) subType.getDeclaredConstructor(Plugin.class).newInstance(this);
                String name = manager.getCodeName();
                getLogger().info("Setting config defaults for manager: " + name);
                ConfigurationSection section = config.getConfigurationSection(name);
                if (section == null) {
                    section = config.createSection(name);
                }
                manager.writeConfigDefaults(section);
                manager.loadConfig(section);
                internalManagers.add(manager);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        internalManagers.sort(Comparator.comparing(Manager::getCodeName));

        List<Class<?>> abilityInfoSubTypes =
                reflections.get(SubTypes.of(AbilityInfo.class).asClass()).stream().filter(c -> c.isAnnotationPresent(Instantiable.class)).sorted(Comparator.comparing(Class::getName)).toList();
        for (Class<?> subType : abilityInfoSubTypes) {
            try {
                AbilityInfo abilityInfo = (AbilityInfo) subType.getDeclaredConstructor(Plugin.class).newInstance(this);
                String codeName = abilityInfo.getCodeName();
                getLogger().info("Setting config defaults for ability: " + codeName);
                ConfigurationSection section = config.getConfigurationSection(codeName);
                if (section == null) section = config.createSection(codeName);
                abilityInfo.writeConfigDefaults(section);
                abilityInfo.loadConfig(section);
                section.addDefault("players", new ArrayList<String>());
                internalAbilityInfos.add(abilityInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        internalAbilityInfos.sort(Comparator.comparing(AbilityInfo::getCodeName));

        getLogger().info("Saving config");

        config.addDefault("no-cooldown", false);
        config.addDefault("dynamic", false);
        config.options().copyDefaults(true);
        saveConfig();

        getLogger().info("Setting up managers");

        for (Manager manager : managers) {
            try {
                getServer().getPluginManager().registerEvents(manager, this);
                getLogger().info("Setting up \"" + manager.getCodeName() + "\"");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        getLogger().info("Setting up abilities");

        PluginCommand command = this.getCommand("ability");
        if (command != null) {
            command.setExecutor(new AbilityCommand());
            command.setTabCompleter(new AbilityTabCompleter());
        } else {
            getServer().broadcastMessage("Failed to register /ability command. Please check plugin description file.");
        }

        if (config.getBoolean("no-cooldown")) {
            getLogger().info("No cooldown mode is ON. All ability cooldowns are set to 1s.");
        }

        for (AbilityInfo abilityInfo : internalAbilityInfos) {
            try {
                String codeName = abilityInfo.getCodeName();
                ConfigurationSection configSection = config.getConfigurationSection(codeName);
                if (configSection == null) configSection = config.createSection(codeName);
                List<String> players = configSection.getStringList("players");
                for (String player : players) {
                    Ability<?> instance = abilityInfo.createInstance(player);
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
                    for (Manager manager : managers) {
                        getLogger().info("Calling onEnable for \"" + manager.getCodeName() + "\"");
                        manager.onEnable();
                    }
                    for (Ability<?> ability : abilities) {
                        getLogger().info("Calling onEnable for \"" + ability.getInfo().getCodeName() + "\" for " + ability.getOwnerName());
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
            for (Ability<?> ability : abilities) {
                getLogger().info("Calling onDisable for \"" + ability.getInfo().getCodeName() + "\" for " + ability.getOwnerName());
                ability.onDisable();
            }
        }

        synchronized (managers) {
            for (Manager manager : managers) {
                getLogger().info("Calling onDisable for \"" + manager.getCodeName() + "\"");
                manager.onDisable();
            }
        }
        getLogger().info("Plugin onDisable completed");
    }

    public void addListener(EventListener listener) {
        internalEventListeners.add(listener);
    }

    public boolean removeListener(EventListener listener) {
        return internalEventListeners.remove(listener);
    }

    @SuppressWarnings("unchecked")
    public <TListener extends EventListener, TEvent extends Event> void emitEvent(Class<TListener> listenerClass, TEvent event, BiConsumer<TListener, TEvent> emitter) {
        for (EventListener listener : internalEventListeners) {
            if (listenerClass.isInstance(listener)) {
                emitter.accept((TListener) listener, event);
            }
        }
    }

    public void removeAbility(Ability<?> ability) {
        synchronized (abilities) {
            if (!abilities.contains(ability)) {
                return;
            }
            abilities.remove(ability);
            HandlerList.unregisterAll(ability);
            ability.onDisable();
            getLogger().info("Calling onDisable for \"" + ability.getInfo().getCodeName() + "\"");
            ConfigurationSection section = config.getConfigurationSection(ability.getInfo().getCodeName());
            if (section != null) {
                List<String> players = section.getStringList("players");
                players.remove(ability.getOwnerName());
                section.set("players", players);
                saveConfig();
            }
        }
    }

    public Ability<?> addAbility(AbilityInfo abilityInfo, String ownerName) {
        try {
            String codeName = abilityInfo.getCodeName();
            ConfigurationSection configSection = config.getConfigurationSection(codeName);
            if (configSection == null) {
                configSection = config.createSection(codeName);
                abilityInfo.writeConfigDefaults(configSection);
                abilityInfo.loadConfig(configSection);
            }
            List<String> players = configSection.getStringList("players");
            if (!players.contains(ownerName)) {
                players.add(ownerName);
                configSection.set("players", players);
                saveConfig();
            }
            Ability<?> instance = abilityInfo.createInstance(ownerName);
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
