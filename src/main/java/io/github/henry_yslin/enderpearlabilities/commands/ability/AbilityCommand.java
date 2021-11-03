package io.github.henry_yslin.enderpearlabilities.commands.ability;

import io.github.henry_yslin.enderpearlabilities.commands.CommandRegistry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class AbilityCommand implements CommandExecutor {

    private final CommandRegistry registry = new CommandRegistry();

    public AbilityCommand() {
        registry.registerCommand(new QueryAbilitySubCommand());
        registry.registerCommand(new InfoAbilitySubCommand());
        registry.registerCommand(new RegisterAbilitySubCommand());
        registry.registerCommand(new UnregisterAbilitySubCommand());
        registry.registerCommand(new ListAbilitySubCommand());
        registry.registerCommand(new ConfigAbilitySubCommand());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return registry.executeCommand(sender, command, label, args);
    }
}
