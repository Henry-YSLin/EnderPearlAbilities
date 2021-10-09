package io.github.henryyslin.enderpearlabilities.commands;

import io.github.henryyslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henryyslin.enderpearlabilities.abilities.Ability;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class UnregisterAbilitySubCommand extends SubCommand {
    protected UnregisterAbilitySubCommand() {
        super("unregister");
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String subcommand, List<String> args) {
        if (args.size() <= 0) return false;
        if (!(sender instanceof Player)) return false;

        List<Ability> abilities;
        synchronized (abilities = EnderPearlAbilities.getInstance().getAbilities()) {
            for (Ability ability : abilities) {
                if (ability.ownerName.equals(sender.getName())) {
                    if (ability.getInfo().codeName.equals(args.get(0))) {
                        EnderPearlAbilities.getInstance().removeAbility(ability);
                        sender.sendMessage("" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "Unregistered ability " + ability.getInfo().codeName);
                        return true;
                    }
                }
            }
        }

        sender.sendMessage(ChatColor.RED + "Cannot find registered ability with code name " + args.get(0));
        return true;
    }
}
