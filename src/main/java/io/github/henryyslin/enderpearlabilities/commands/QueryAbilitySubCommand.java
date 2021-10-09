package io.github.henryyslin.enderpearlabilities.commands;

import io.github.henryyslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henryyslin.enderpearlabilities.abilities.Ability;
import io.github.henryyslin.enderpearlabilities.utils.AbilityUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class QueryAbilitySubCommand extends SubCommand {
    protected QueryAbilitySubCommand() {
        super("query");
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String subcommand, List<String> args) {
        String lookupPlayer = null;
        if (args.size() >= 1)
            lookupPlayer = args.get(0);
        else if (sender instanceof Player)
            lookupPlayer = sender.getName();

        if (lookupPlayer == null || lookupPlayer.length() == 0)
            return false;

        String finalLookupPlayer = lookupPlayer;
        List<Ability> abilities;
        List<Ability> targetAbilities;
        synchronized (abilities = EnderPearlAbilities.getInstance().getAbilities()) {
            targetAbilities = abilities.stream().filter(x -> x.ownerName.equals(finalLookupPlayer)).collect(Collectors.toList());
        }
        if (targetAbilities.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Cannot find " + finalLookupPlayer + "'s ability");
        } else {
            for (Ability ability : targetAbilities) {
                sender.sendMessage(AbilityUtils.formatAbilityInfo(ability));
            }
        }

        return true;
    }
}
