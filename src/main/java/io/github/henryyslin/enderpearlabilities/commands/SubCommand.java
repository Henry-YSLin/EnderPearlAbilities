package io.github.henryyslin.enderpearlabilities.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import javax.annotation.Nullable;
import java.util.List;

public abstract class SubCommand {
    protected final String subCommandName;

    protected SubCommand(@Nullable String subCommandName) {
        this.subCommandName = subCommandName;
    }

    public abstract boolean execute(CommandSender sender, Command command, String label, String subcommand, List<String> args);

    @Nullable
    public String getSubCommandName() {
        return subCommandName;
    }
}
