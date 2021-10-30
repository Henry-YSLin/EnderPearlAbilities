package io.github.henryyslin.enderpearlabilities.commands.ability;

import io.github.henryyslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henryyslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class AbilityTabCompleter implements TabCompleter {

    private static final String[] STATIC_COMMANDS = {"config", "info", "list", "query"};
    private static final String[] DYNAMIC_COMMANDS = {"register", "unregister"};
    private static final String[] PLAYER_COMMANDS = {"query"};
    private static final String[] ABILITY_COMMANDS = {"info"};

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partialCommand = args[0];
            List<String> commands = new ArrayList<>(Arrays.asList(STATIC_COMMANDS));
            if (EnderPearlAbilities.getInstance().getLoadedConfig().getBoolean("dynamic"))
                commands.addAll(Arrays.asList(DYNAMIC_COMMANDS));
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
            } else if (args[0].equals("register")) {
                String partialAbility = args[1];
                List<ActivationHand> occupiedHands = EnderPearlAbilities.getInstance().getAbilities().stream()
                        .filter(ability -> Objects.equals(ability.ownerName, sender.getName()))
                        .map(ability -> ability.getInfo().activation)
                        .distinct()
                        .toList();
                List<String> abilities = EnderPearlAbilities.getInstance().getTemplateAbilities().stream()
                        .filter(ability -> !occupiedHands.contains(ability.getInfo().activation))
                        .map(ability -> ability.getInfo().codeName)
                        .toList();
                StringUtil.copyPartialMatches(partialAbility, abilities, completions);
            } else if (args[0].equals("unregister")) {
                String partialAbility = args[1];
                List<String> abilities = EnderPearlAbilities.getInstance().getAbilities().stream()
                        .filter(ability -> Objects.equals(ability.ownerName, sender.getName()))
                        .map(ability -> ability.getInfo().codeName)
                        .toList();
                StringUtil.copyPartialMatches(partialAbility, abilities, completions);
            }
        }

        Collections.sort(completions);

        return completions;
    }
}
