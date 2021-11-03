package io.github.henry_yslin.enderpearlabilities.commands.ability;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henry_yslin.enderpearlabilities.commands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

public class RegisterAbilitySubCommand extends SubCommand {

    protected RegisterAbilitySubCommand() {
        super("register");
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String subcommand, List<String> args) {
        if (!EnderPearlAbilities.getInstance().getLoadedConfig().getBoolean("dynamic")) {
            sender.sendMessage(ChatColor.RED + "This command is only allowed when dynamic abilities are on.");
            return true;
        }

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
                if (Objects.equals(ability.ownerName, sender.getName())) {
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
