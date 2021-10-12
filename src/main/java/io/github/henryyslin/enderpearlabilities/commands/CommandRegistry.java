package io.github.henryyslin.enderpearlabilities.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Stores and executes {@link SubCommand}s that are from the same main command.
 */
public class CommandRegistry {
    private final HashSet<SubCommand> registeredCommands = new HashSet<>();

    /**
     * Add a sub-command to the store so that it can be executed.
     *
     * @param command The {@link SubCommand} to add.
     */
    public void registerCommand(SubCommand command) {
        registeredCommands.add(command);
    }

    /**
     * Select and execute a registered {@link SubCommand} by matching the command arguments.
     * Supply the same parameters as the {@code onCommand} method of an {@link org.bukkit.command.CommandExecutor}.
     *
     * @return Whether the {@link SubCommand} has executed successfully. Refer to {@code onCommand} of {@link org.bukkit.command.CommandExecutor} for details.
     */
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