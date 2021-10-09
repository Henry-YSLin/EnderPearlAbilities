package io.github.henryyslin.enderpearlabilities.commands;

import io.github.henryyslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henryyslin.enderpearlabilities.abilities.Ability;
import io.github.henryyslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henryyslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class RegisterAbilitySubCommand extends SubCommand {
    protected RegisterAbilitySubCommand() {
        super("register");
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String subcommand, List<String> args) {
        if (args.size() <= 0) return false;
        if (!(sender instanceof Player)) return false;

        Ability templateAbility = null;
        List<Ability> templateAbilities = EnderPearlAbilities.getInstance().getTemplateAbilities();
        for (Ability template : templateAbilities) {
            AbilityInfo info = template.getInfo();
            if (info.codeName.equals(args.get(0))) {
                templateAbility = template;
            }
        }

        if (templateAbility == null) {
            sender.sendMessage(ChatColor.RED + "Cannot find ability with code name " + args.get(0));
            return true;
        }

        List<Ability> abilities;
        synchronized (abilities = EnderPearlAbilities.getInstance().getAbilities()) {
            for (Ability ability : abilities) {
                if (ability.ownerName.equals(sender.getName())) {
                    if (ability.getInfo().codeName.equals(args.get(0))) {
                        sender.sendMessage(ChatColor.RED + "You already have this ability registered");
                        return true;
                    }
                    if (ability.getInfo().activation == templateAbility.getInfo().activation) {
                        sender.sendMessage(ChatColor.RED + "Your " + (ability.getInfo().activation == ActivationHand.MainHand ? "main" : "off") + " hand is already occupied by the ability " + ability.getInfo().codeName);
                        return true;
                    }
                }
            }
        }

        EnderPearlAbilities.getInstance().addAbility(templateAbility, sender.getName());

        sender.sendMessage("" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "Registered ability " + templateAbility.getInfo().codeName);
        return true;
    }
}
