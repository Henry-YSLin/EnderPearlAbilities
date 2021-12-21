package io.github.henry_yslin.enderpearlabilities.commands.ability;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.commands.SubCommand;
import io.github.henry_yslin.enderpearlabilities.utils.PlayerUtils;
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
        if (!PlayerUtils.checkPermissionOrError(sender, "ability." + subCommandName)) return true;
        FileConfiguration config = EnderPearlAbilities.getInstance().getLoadedConfig();
        sender.sendMessage("no-cooldown: " + config.getBoolean("no-cooldown"));
        sender.sendMessage("dynamic: " + config.getBoolean("dynamic"));
        return true;
    }
}
