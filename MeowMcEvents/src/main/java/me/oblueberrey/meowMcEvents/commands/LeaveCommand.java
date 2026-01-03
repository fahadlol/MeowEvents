package me.oblueberrey.meowMcEvents.commands;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveCommand implements CommandExecutor {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;

    public LeaveCommand(MeowMCEvents plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    private void debug(String message) {
        if (plugin.getConfigManager().shouldLogPlayers()) {
            plugin.getLogger().info("[DEBUG:LEAVE] " + message);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        // Check if player is in event (active) or has joined the queue (waiting)
        boolean inActiveEvent = eventManager.isPlayerInEvent(player);
        boolean inQueue = eventManager.hasPlayerJoined(player);

        if (!inActiveEvent && !inQueue) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfigManager().getMessage("not-in-event")));
            return true;
        }

        debug(player.getName() + " is leaving. InActiveEvent: " + inActiveEvent + ", InQueue: " + inQueue);

        // Remove from event
        eventManager.removePlayer(player);

        // Only teleport and clear inventory if player was in active event
        if (inActiveEvent) {
            Location playerSpawn = plugin.getConfigManager().getPlayerSpawnLocation();
            if (playerSpawn != null && playerSpawn.getWorld() != null) {
                player.teleport(playerSpawn);
            }
            player.getInventory().clear();
            player.setHealth(20.0);
            player.setFoodLevel(20);
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfigManager().getMessage("left-event")));

        // Check for winner after player leaves (only if event is running)
        if (eventManager.isEventRunning()) {
            debug("Checking for winner after " + player.getName() + " left");
            eventManager.checkForWinner();
        }

        return true;
    }
}