package me.oblueberrey.meowMcEvents.commands;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;

public class EventJoinCommand implements CommandExecutor {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;

    public EventJoinCommand(MeowMCEvents plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    private void debug(String message) {
        if (plugin.getConfigManager().shouldLogPlayers()) {
            plugin.getLogger().info("[DEBUG:JOIN] " + message);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        // Check permission
        if (!player.hasPermission("meowevent.join")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to join events!");
            return true;
        }

        // Check if event is running - allow joining as spectator
        if (eventManager.isEventRunning()) {
            // Check if already a spectator
            if (eventManager.isSpectator(player)) {
                player.sendMessage(ChatColor.RED + "You are already spectating this event!");
                return true;
            }

            // Check if already in the event as a player
            if (eventManager.isPlayerInEvent(player)) {
                player.sendMessage(ChatColor.RED + "You are already in this event!");
                return true;
            }

            // Add as spectator
            eventManager.addSpectator(player);

            // Teleport to event spawn location
            Location eventSpawn = plugin.getConfigManager().getSpawnLocation();
            if (eventSpawn != null && eventSpawn.getWorld() != null) {
                player.teleport(eventSpawn);
                debug(player.getName() + " teleported to event spawn as spectator");
            }

            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6&l[MeowEvent] &eYou are now spectating the event! Use &c/leave &eto exit."));
            return true;
        }

        // Check if countdown is active (this is when players CAN join)
        if (!eventManager.isCountdownActive()) {
            player.sendMessage(ChatColor.RED + "No event is currently accepting players! Wait for an event to be announced.");
            return true;
        }

        // Check if already joined
        if (eventManager.hasPlayerJoined(player)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfigManager().getMessage("already-joined")));
            return true;
        }

        // Add player to event queue
        eventManager.addPlayer(player);

        // Teleport player to event join spawn (waiting area)
        Location eventSpawn = plugin.getConfigManager().getEventJoinSpawnLocation();
        if (eventSpawn != null && eventSpawn.getWorld() != null) {
            player.teleport(eventSpawn);
            debug(player.getName() + " teleported to event join spawn (waiting area)");
        } else {
            debug(player.getName() + " joined but event spawn is not set");
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfigManager().getMessage("joined-event")));

        return true;
    }
}