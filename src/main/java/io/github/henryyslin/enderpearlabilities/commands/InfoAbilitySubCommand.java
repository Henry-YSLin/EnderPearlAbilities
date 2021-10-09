package io.github.henryyslin.enderpearlabilities.commands;

import io.github.henryyslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henryyslin.enderpearlabilities.abilities.Ability;
import io.github.henryyslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henryyslin.enderpearlabilities.utils.AbilityUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public class InfoAbilitySubCommand extends SubCommand {
    protected InfoAbilitySubCommand() {
        super("info");
    }

    private String friendlyNumber(int number) {
        if (number == Integer.MAX_VALUE) return "infinite";
        return String.valueOf(number / 20f);
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String subcommand, List<String> args) {
        if (args.size() <= 0) return false;

        List<Ability> templateAbilities = EnderPearlAbilities.getInstance().getTemplateAbilities();
        for (Ability templateAbility : templateAbilities) {
            AbilityInfo info = templateAbility.getInfo();
            if (info.codeName.equals(args.get(0))) {
                sender.sendMessage(AbilityUtils.formatAbilityInfo(templateAbility));
                return true;
            }
        }

        sender.sendMessage(ChatColor.RED + "Cannot find ability with code name " + args.get(0));
        return true;
    }
}
