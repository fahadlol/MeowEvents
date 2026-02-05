package me.oblueberrey.meowMcEvents.commands;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.utils.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveCommand implements CommandExecutor {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;

    // Improved RGB Colors
    private static final String GREY = "&#AAAAAA";
    private static final String RED = "&#FF5555";
    private static final String YELLOW = "&#FFE566";
    private static final String ORANGE = "&#FF9944";

    public LeaveCommand(MeowMCEvents plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    private String msg(String text) {
        return ConfigManager.colorize(text);
    }

    private void debug(String message) {
        if (plugin.getConfigManager().shouldLogPlayers()) {
            plugin.getLogger().info("[DEBUG:LEAVE] " + message);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg(GREY + "" + RED + "players only"));
            return true;
        }

        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("meowevent.leave")) {
            player.sendMessage(msg("&7&cno permission"));
            return true;
        }

        // Check if player is in event (active), in queue (waiting), or spectating
        boolean inActiveEvent = eventManager.isPlayerInEvent(player);
        boolean inQueue = eventManager.hasPlayerJoined(player);
        boolean isSpectator = eventManager.isSpectator(player);

        if (!inActiveEvent && !inQueue && !isSpectator) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-in-event"));
            return true;
        }

        debug(player.getName() + " is leaving. InActiveEvent: " + inActiveEvent + ", InQueue: " + inQueue + ", IsSpectator: " + isSpectator);

        // Handle spectator leaving
        if (isSpectator) {
            eventManager.removeSpectator(player);
            debug(player.getName() + " removed from spectators");
        } else {
            // Remove from event (for active players or queue)
            eventManager.removePlayer(player);
        }

        // Clear inventory and reset health if player was in active event or spectating
        if (inActiveEvent || isSpectator) {
            player.getInventory().clear();
            player.setHealth(20.0);
            player.setFoodLevel(20);
        }

        player.sendMessage(plugin.getConfigManager().getMessage("left-event"));

        // Execute end command from config to teleport player
        String endCommand = plugin.getConfigManager().getEndCommand();
        if (endCommand != null && !endCommand.isEmpty()) {
            player.performCommand(endCommand);
            debug(player.getName() + " executed /" + endCommand);
        }

        // Check for winner after player leaves (only if event is running)
        if (eventManager.isEventRunning()) {
            debug("Checking for winner after " + player.getName() + " left");
            eventManager.checkForWinner();
        }

        return true;
    }
}