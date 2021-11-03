package io.github.henry_yslin.enderpearlabilities.commands.ability;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henry_yslin.enderpearlabilities.commands.SubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ListAbilitySubCommand extends SubCommand {

    protected ListAbilitySubCommand() {
        super("list");
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String subcommand, List<String> args) {
        List<Ability> abilities;
        synchronized (abilities = EnderPearlAbilities.getInstance().getAbilities()) {
            List<Ability> templateAbilities = EnderPearlAbilities.getInstance().getTemplateAbilities();
            for (Ability templateAbility : templateAbilities) {
                AbilityInfo info = templateAbility.getInfo();
                int count = 0;
                for (Ability ability : abilities) {
                    if (ability.getInfo().codeName.equals(info.codeName)) {
                        count++;
                    }
                }
                if (count > 0)
                    sender.sendMessage(info.codeName + " - " + count + " active");
                else
                    sender.sendMessage(info.codeName);
            }
        }

        return true;
    }
}
