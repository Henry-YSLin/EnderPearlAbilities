package io.github.henryyslin.enderpearlabilities;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Optional;

public record CommandAbility(FileConfiguration config,
                             Ability[] abilities) implements CommandExecutor {

    private String friendlyNumber(int number) {
        if (number == Integer.MAX_VALUE) return "infinite";
        return String.valueOf(number / 20f);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String lookupPlayer = null;
        if (args.length >= 1)
            lookupPlayer = args[0];
        else if (sender instanceof Player)
            lookupPlayer = sender.getName();

        if (lookupPlayer == null)
            return false;

        String finalLookupPlayer = lookupPlayer;
        Optional<Ability> targetAbility = Arrays.stream(abilities).filter(x -> finalLookupPlayer.equals(config.getString(x.getConfigName()))).findFirst();
        if (targetAbility.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Cannot find " + finalLookupPlayer + "'s ability");
        } else {
            Ability ability = targetAbility.get();
            sender.sendMessage(
                    String.format("%s - %s%s%s - %s", finalLookupPlayer, ChatColor.LIGHT_PURPLE, ChatColor.BOLD, ability.getOrigin(), ability.getName()),
                    ability.getDescription(),
                    String.format("%sActivation: %s", ChatColor.GRAY, ability.getActivation() == ActivationHand.MainHand ? "main hand" : "off hand"),
                    String.format("%sCharge up: %ss   Duration: %ss   Cool down: %ss", ChatColor.GRAY, friendlyNumber(ability.getChargeUp()), friendlyNumber(ability.getDuration()), friendlyNumber(ability.getCooldown()))
            );
        }

        return true;
    }
}
