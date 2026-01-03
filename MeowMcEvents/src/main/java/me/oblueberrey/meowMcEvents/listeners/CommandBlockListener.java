package me.oblueberrey.meowMcEvents.listeners;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandBlockListener implements Listener {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;

    public CommandBlockListener(MeowMCEvents plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    private void debug(String message) {
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG:CMD] " + message);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        // Only block commands if player is in event
        if (!eventManager.isPlayerInEvent(player)) {
            return;
        }

        String command = event.getMessage().toLowerCase();

        // Allow /leave command
        if (command.startsWith("/leave")) {
            debug(player.getName() + " used /leave command - allowed");
            return;
        }

        // Allow admin commands
        if (player.hasPermission("meowevent.admin")) {
            debug(player.getName() + " (admin) used command: " + command + " - allowed");
            return;
        }

        // Block all other commands
        event.setCancelled(true);
        debug(player.getName() + " tried to use command: " + command + " - blocked");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfigManager().getMessage("command-blocked")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfigManager().getMessage("command-allowed")));
    }
}