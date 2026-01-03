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

        // Check if player is in event (active), in queue (waiting), or spectating
        boolean inActiveEvent = eventManager.isPlayerInEvent(player);
        boolean inQueue = eventManager.hasPlayerJoined(player);
        boolean isSpectator = eventManager.isSpectator(player);

        if (!inActiveEvent && !inQueue && !isSpectator) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfigManager().getMessage("not-in-event")));
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

        // Always teleport to player spawn when leaving
        Location playerSpawn = plugin.getConfigManager().getPlayerSpawnLocation();
        if (playerSpawn != null && playerSpawn.getWorld() != null) {
            player.teleport(playerSpawn);
            debug(player.getName() + " teleported to player spawn");
        }

        // Clear inventory and reset health if player was in active event
        if (inActiveEvent) {
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