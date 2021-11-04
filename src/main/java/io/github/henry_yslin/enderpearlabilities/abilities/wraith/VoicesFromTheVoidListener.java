package io.github.henry_yslin.enderpearlabilities.abilities.wraith;

import io.github.henry_yslin.enderpearlabilities.ExtendedListener;
import io.github.henry_yslin.enderpearlabilities.ExtendedRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("rawtypes")
public class VoicesFromTheVoidListener extends ExtendedListener<ExtendedRunnable> {
    private final Ability ability;

    public VoicesFromTheVoidListener(Plugin plugin, Ability ability, @Nullable ConfigurationSection config) {
        super(plugin, config);
        this.ability = ability;
    }

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        super.setConfigDefaults(config);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    private void sendVoice(String message) {
        ability.player.sendMessage(ChatColor.GRAY + "<?> " + ChatColor.LIGHT_PURPLE + message);
    }

    @EventHandler
    public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player player)) return;
        if (!player.getName().equals(ability.ownerName)) return;
        if (!(event.getEntity() instanceof Monster)) return;
        sendVoice("Danger, move!" + Math.random());
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
