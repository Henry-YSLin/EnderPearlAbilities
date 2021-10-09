package io.github.henryyslin.enderpearlabilities.commands;

import io.github.henryyslin.enderpearlabilities.EnderPearlAbilities;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigAbilitySubCommand extends SubCommand {
    protected ConfigAbilitySubCommand() {
        super("config");
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String subcommand, List<String> args) {
        FileConfiguration config = EnderPearlAbilities.getInstance().getLoadedConfig();
        sender.sendMessage("no-cooldown: " + config.getBoolean("no-cooldown"));
        sender.sendMessage("dynamic: " + config.getBoolean("dynamic"));
        return true;
    }
}
