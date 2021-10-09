package io.github.henryyslin.enderpearlabilities.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

public class CommandRegistry {
    private final HashSet<SubCommand> registeredCommands = new HashSet<>();

    public void registerCommand(SubCommand command) {
        registeredCommands.add(command);
    }

    public boolean executeCommand(CommandSender sender, Command cmd, String label, String[] args) {
        for (SubCommand registeredCommand : registeredCommands) {
            if (registeredCommand.getSubCommandName() == null && (args.length <= 0 || args[0] == null || args[0].length() == 0)) {
                return registeredCommand.execute(sender, cmd, label, args[0], Arrays.stream(args).skip(1).collect(Collectors.toList()));
            }
            if (registeredCommand.getSubCommandName() != null && args.length > 0 && registeredCommand.getSubCommandName().equalsIgnoreCase(args[0])) {
                return registeredCommand.execute(sender, cmd, label, args[0], Arrays.stream(args).skip(1).collect(Collectors.toList()));
            }
        }
        return false;
    }
}