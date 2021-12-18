package io.github.henry_yslin.enderpearlabilities.commands.ability;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.commands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

public class UnregisterAbilitySubCommand extends SubCommand {

    protected UnregisterAbilitySubCommand() {
        super("unregister");
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String subcommand, List<String> args) {
        if (!EnderPearlAbilities.getInstance().getLoadedConfig().getBoolean("dynamic")) {
            sender.sendMessage(ChatColor.RED + "This command is only allowed when dynamic abilities are on.");
            return true;
        }

        if (args.size() <= 0) return false;
        if (!(sender instanceof Player)) return false;

        List<Ability<?>> abilities;
        synchronized (abilities = EnderPearlAbilities.getInstance().getAbilities()) {
            for (Ability<?> ability : abilities) {
                if (Objects.equals(ability.getOwnerName(), sender.getName())) {
                    if (ability.getInfo().getCodeName().equalsIgnoreCase(args.get(0))) {
                        EnderPearlAbilities.getInstance().removeAbility(ability);
                        sender.sendMessage("" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "Unregistered ability " + ability.getInfo().getCodeName());
                        return true;
                    }
                }
            }
        }

        sender.sendMessage(ChatColor.RED + "Cannot find registered ability with code name " + args.get(0));
        return true;
    }
}
