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

        // Block commands if player is in active event OR in waiting area (joined during countdown)
        boolean inActiveEvent = eventManager.isPlayerInEvent(player);
        boolean inWaitingArea = eventManager.isCountdownActive() && eventManager.hasPlayerJoined(player);

        if (!inActiveEvent && !inWaitingArea) {
            return;
        }

        String command = event.getMessage().toLowerCase();

        // Allow /leave command for everyone - highest priority, explicitly un-cancel
        if (command.startsWith("/leave")) {
            event.setCancelled(false); // Force allow /leave even if other plugins cancelled it
            debug(player.getName() + " used /leave command - allowed (forced priority)");
            return;
        }

        // Allow commands for OPs
        if (player.isOp()) {
            debug(player.getName() + " (OP) used command: " + command + " - allowed");
            return;
        }

        // Allow commands for admins with permission
        if (player.hasPermission("meowevent.admin")) {
            debug(player.getName() + " (admin) used command: " + command + " - allowed");
            return;
        }

        // Block all other commands for regular players
        event.setCancelled(true);
        String state = inActiveEvent ? "event" : "waiting area";
        debug(player.getName() + " tried to use command: " + command + " in " + state + " - blocked");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfigManager().getMessage("command-blocked")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfigManager().getMessage("command-allowed")));
    }
}