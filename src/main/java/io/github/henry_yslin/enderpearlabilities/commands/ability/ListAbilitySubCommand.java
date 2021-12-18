package io.github.henry_yslin.enderpearlabilities.commands.ability;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henry_yslin.enderpearlabilities.commands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ListAbilitySubCommand extends SubCommand {

    protected ListAbilitySubCommand() {
        super("list");
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String subcommand, List<String> args) {
        List<Ability<?>> abilities;
        synchronized (abilities = EnderPearlAbilities.getInstance().getAbilities()) {
            List<AbilityInfo> abilityInfos = EnderPearlAbilities.getInstance().getAbilityInfos();
            int totalCount = 0;

            for (ActivationHand hand : ActivationHand.values()) {
                List<AbilityInfo> filteredInfos = abilityInfos.stream().filter(info -> info.getActivation() == hand).toList();
                sender.sendMessage(ChatColor.GRAY + hand.toString() + " abilities:");

                for (AbilityInfo info : filteredInfos) {
                    long count = abilities.stream().filter(a -> a.getInfo().getCodeName().equals(info.getCodeName())).count();
                    if (count > 0) {
                        sender.sendMessage(info.getCodeName() + " - " + count + " instance" + (count > 1 ? "s" : "") + " active");
                    } else {
                        sender.sendMessage(info.getCodeName());
                    }
                    totalCount += count;
                }
            }

            sender.sendMessage("Total: " + abilityInfos.size() + " abilities, " + totalCount + " active instances");
        }

        return true;
    }
}
