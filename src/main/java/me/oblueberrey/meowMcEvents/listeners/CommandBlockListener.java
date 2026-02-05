package me.oblueberrey.meowMcEvents.listeners;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.utils.EventState;
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

    // LOWEST priority to run BEFORE other plugins (like spawn plugins that give items)
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        // Allow all commands during event cleanup/ending phase
        EventState currentState = eventManager.getState();
        if (currentState == EventState.ENDING || currentState == EventState.IDLE) {
            return;
        }

        // Block commands if player is in active event, spectating, or in waiting area
        boolean inActiveEvent = eventManager.isPlayerInEvent(player);
        boolean isSpectator = eventManager.isSpectator(player);
        boolean inWaitingArea = eventManager.isCountdownActive() && eventManager.hasPlayerJoined(player);

        if (!inActiveEvent && !isSpectator && !inWaitingArea) {
            return;
        }

        String command = event.getMessage().toLowerCase();

        // Block /spawn for ALL event participants (players, spectators, waiting) - no exceptions
        if (command.startsWith("/spawn")) {
            event.setCancelled(true);
            debug(player.getName() + " tried to use /spawn during event - blocked");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfigManager().getMessage("command-blocked")));
            return;
        }

        // Check against blacklisted commands - ALWAYS blocked, even for admins/OPs
        java.util.List<String> blacklisted = plugin.getConfigManager().getBlacklistedCommands();
        for (String blocked : blacklisted) {
            if (command.startsWith("/" + blocked.toLowerCase())) {
                event.setCancelled(true);
                debug(player.getName() + " tried blacklisted command: " + command + " - blocked");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfigManager().getMessage("command-blocked")));
                return;
            }
        }

        // Check against whitelisted commands from config
        java.util.List<String> whitelisted = plugin.getConfigManager().getWhitelistedCommands();
        for (String allowed : whitelisted) {
            if (command.startsWith("/" + allowed.toLowerCase())) {
                event.setCancelled(false);
                debug(player.getName() + " used whitelisted command: " + command);
                return;
            }
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
        String state = inActiveEvent ? "event" : (isSpectator ? "spectating" : "waiting area");
        debug(player.getName() + " tried to use command: " + command + " in " + state + " - blocked");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfigManager().getMessage("command-blocked")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfigManager().getMessage("command-allowed")));
    }
}