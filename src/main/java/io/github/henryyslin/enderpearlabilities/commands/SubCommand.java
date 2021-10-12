package io.github.henryyslin.enderpearlabilities.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents a command that can be executed by supplying an additional verb to the main command label.
 * For example, {@code /ability} is a main command, and {@code /ability list} is a sub-command.
 */
public abstract class SubCommand {
    protected final String subCommandName;

    /**
     * Create a new instance of {@link SubCommand} with a sub-command name for matching.
     *
     * @param subCommandName The name of this sub-command, also the verb that is used to execute this sub-command. Use null to execute this sub-command when a verb is missing.
     */
    protected SubCommand(@Nullable String subCommandName) {
        this.subCommandName = subCommandName;
    }

    /**
     * Execute the sub-command. Parameters are similar to {@code onCommand} of {@link org.bukkit.command.CommandExecutor},
     * with the addition of {@code subcommand} being the extra verb used to execute this sub-command (should be equal to
     * {@code subCommandName}). The first element of {@code args} is the first item after {@code subcommand} (thus {@code subcommand}
     * itself is excluded from the {@code args} list).
     *
     * @return Whether the {@link SubCommand} has executed successfully. Refer to {@code onCommand} of {@link org.bukkit.command.CommandExecutor} for details.
     */
    public abstract boolean execute(CommandSender sender, Command command, String label, String subcommand, List<String> args);

    /**
     * The name of this sub-command, also the verb that is used to execute this sub-command.
     * Can be null if this sub-command should be executed when a verb is missing.
     */
    @Nullable
    public String getSubCommandName() {
        return subCommandName;
    }
}
