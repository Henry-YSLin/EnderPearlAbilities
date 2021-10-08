package io.github.henryyslin.enderpearlabilities;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public record CommandAbility(FileConfiguration config,
                             List<Ability> abilities) implements CommandExecutor {

    private String friendlyNumber(int number) {
        if (number == Integer.MAX_VALUE) return "infinite";
        return String.valueOf(number / 20f);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String lookupPlayer = null;
        if (args.length >= 1)
            lookupPlayer = args[0];
        else if (sender instanceof Player)
            lookupPlayer = sender.getName();

        if (lookupPlayer == null || lookupPlayer.length() == 0)
            return false;

        String finalLookupPlayer = lookupPlayer;
        Optional<Ability> targetAbility = abilities.stream().filter(x -> config.getStringList(x.getInfo().codeName).contains(finalLookupPlayer)).findFirst();
        if (targetAbility.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Cannot find " + finalLookupPlayer + "'s ability");
        } else {
            AbilityInfo info = targetAbility.get().getInfo();
            sender.sendMessage(
                    String.format("%s - %s%s%s - %s", finalLookupPlayer, ChatColor.LIGHT_PURPLE, ChatColor.BOLD, info.origin, info.name),
                    info.description,
                    String.format("%sActivation: %s", ChatColor.GRAY, info.activation == ActivationHand.MainHand ? "main hand" : "off hand"),
                    String.format("%sCharge up: %ss   Duration: %ss   Cool down: %ss", ChatColor.GRAY, friendlyNumber(info.chargeUp), friendlyNumber(info.duration), friendlyNumber(info.cooldown))
            );
        }

        return true;
    }
}
