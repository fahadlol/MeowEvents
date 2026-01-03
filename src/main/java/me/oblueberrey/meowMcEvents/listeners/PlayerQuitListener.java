package me.oblueberrey.meowMcEvents.listeners;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.managers.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final EventManager eventManager;
    private final TeamManager teamManager;

    public PlayerQuitListener(EventManager eventManager, TeamManager teamManager) {
        this.eventManager = eventManager;
        this.teamManager = teamManager;
    }

    private void debug(String message) {
        MeowMCEvents plugin = MeowMCEvents.getInstance();
        if (plugin != null && plugin.getConfigManager().shouldLogPlayers()) {
            plugin.getLogger().info("[DEBUG:QUIT] " + message);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Check if player is a spectator - just remove them silently
        if (eventManager.isSpectator(player)) {
            eventManager.removeSpectator(player);
            debug(player.getName() + " (spectator) disconnected and removed");
            return;
        }

        // Check if player is in event
        if (!eventManager.isPlayerInEvent(player)) {
            return;
        }

        debug(player.getName() + " disconnected while in event");

        // Broadcast disconnect message
        Bukkit.broadcastMessage(ChatColor.YELLOW + player.getName() +
                ChatColor.GRAY + " disconnected and has been eliminated from the event!");

        // Remove player from event
        eventManager.removePlayer(player);
        debug(player.getName() + " removed from event due to disconnect");

        // Check for winner after disconnect
        if (eventManager.isEventRunning()) {
            debug("Checking for winner after " + player.getName() + " disconnect");
            eventManager.checkForWinner();
        }
    }
}