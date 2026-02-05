package me.oblueberrey.meowMcEvents.commands;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.utils.MessageUtils;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
            MessageUtils.sendError(sender, "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        // Check permission
        if (!player.hasPermission("meowevent.join")) {
            player.sendMessage(plugin.getConfigManager().colorize("&7&cno permission"));
            return true;
        }

        // Check if event is running - allow joining as spectator
        if (eventManager.isEventRunning()) {
            // Check if already a spectator
            if (eventManager.isSpectator(player)) {
                MessageUtils.sendError(player, "You are already spectating this event.");
                return true;
            }

            // Check if already in the event as a player
            if (eventManager.isPlayerInEvent(player)) {
                MessageUtils.sendError(player, "You are already in this event.");
                return true;
            }

            // Check if mid-join spectating is allowed
            if (!plugin.getConfigManager().isAllowMidJoinSpectate()) {
                MessageUtils.sendError(player, "Spectating is not allowed for this event.");
                return true;
            }

            // Add as spectator
            eventManager.addSpectator(player);

            // Teleport to event spawn location
            Location eventSpawn = plugin.getConfigManager().getSpawnLocation();
            if (eventSpawn != null && eventSpawn.getWorld() != null) {
                player.teleport(eventSpawn);
                debug(player.getName() + " Teleported to event spawn as spectator");
            }

            MessageUtils.sendInfo(player, "You are now spectating. Use &f/leave &7to exit.");
            return true;
        }

        // Check if countdown is active (this is when players CAN join)
        if (!eventManager.isCountdownActive()) {
            MessageUtils.sendError(player, "There is no ongoing event or countdown!");
            return true;
        }

        // Check if already joined
        if (eventManager.hasPlayerJoined(player)) {
            player.sendMessage(plugin.getConfigManager().getMessage("already-joined"));
            return true;
        }

        // Check max players limit
        int maxPlayers = plugin.getConfigManager().getMaxPlayers();
        if (maxPlayers > 0 && eventManager.getJoinedPlayerCount() >= maxPlayers) {
            MessageUtils.sendError(player, "The event is full! (" + maxPlayers + " players max)");
            return true;
        }

        // Add player to event queue
        eventManager.addPlayer(player);

        // Teleport player to event join spawn (waiting area)
        Location eventSpawn = plugin.getConfigManager().getEventJoinSpawnLocation();
        if (eventSpawn != null && eventSpawn.getWorld() != null) {
            player.teleport(eventSpawn);
            debug(player.getName() + " teleported to event join spawn (waiting area)");
        }

        player.sendMessage(plugin.getConfigManager().getMessage("joined-event"));

        return true;
    }
}