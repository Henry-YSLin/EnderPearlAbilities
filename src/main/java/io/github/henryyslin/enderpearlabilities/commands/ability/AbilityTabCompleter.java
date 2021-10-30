package io.github.henryyslin.enderpearlabilities.commands.ability;

import io.github.henryyslin.enderpearlabilities.EnderPearlAbilities;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AbilityTabCompleter implements TabCompleter {

    private static final String[] COMMANDS = {"config", "info", "list", "query", "register", "unregister"};
    private static final String[] PLAYER_COMMANDS = {"query"};
    private static final String[] ABILITY_COMMANDS = {"info", "register", "unregister"};

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partialCommand = args[0];
            List<String> commands = new ArrayList<>(Arrays.asList(COMMANDS));
            StringUtil.copyPartialMatches(partialCommand, commands, completions);
        }

        if (args.length == 2) {
            if (Arrays.stream(PLAYER_COMMANDS).anyMatch(cmd -> cmd.equals(args[0]))) {
                String partialPlayer = args[1];
                List<String> players = Arrays.stream(EnderPearlAbilities.getInstance().getServer().getOfflinePlayers()).map(OfflinePlayer::getName).toList();
                StringUtil.copyPartialMatches(partialPlayer, players, completions);
            } else if (Arrays.stream(ABILITY_COMMANDS).anyMatch(cmd -> cmd.equals(args[0]))) {
                String partialAbility = args[1];
                List<String> abilities = EnderPearlAbilities.getInstance().getTemplateAbilities().stream().map(ability -> ability.getInfo().codeName).toList();
                StringUtil.copyPartialMatches(partialAbility, abilities, completions);
            }
        }

        Collections.sort(completions);

        return completions;
    }
}
