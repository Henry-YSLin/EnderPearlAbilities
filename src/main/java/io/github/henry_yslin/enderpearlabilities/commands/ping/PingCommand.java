package io.github.henry_yslin.enderpearlabilities.commands.ping;

import io.github.henry_yslin.enderpearlabilities.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PingCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!PlayerUtils.checkPermissionOrError(sender, "ping")) return true;
        String lookupPlayer = null;
        if (args.length >= 1)
            lookupPlayer = args[0];
        else if (sender instanceof Player)
            lookupPlayer = sender.getName();

        if (lookupPlayer == null || lookupPlayer.length() == 0)
            return false;

        Player player = Bukkit.getPlayer(lookupPlayer);
        if (player == null || !player.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player not found or is not online.");
            return true;
        }
        sender.sendMessage("Ping of " + player.getName() + ": " + player.getPing() + "ms");
        return true;
    }
}
