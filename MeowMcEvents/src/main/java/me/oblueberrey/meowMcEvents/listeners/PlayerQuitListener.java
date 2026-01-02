package me.oblueberrey.meowMcEvents.listeners;

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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!eventManager.isEventRunning()) {
            return;
        }

        Player player = event.getPlayer();

        // Broadcast disconnect message
        Bukkit.broadcastMessage(ChatColor.YELLOW + player.getName() +
                ChatColor.GRAY + " disconnected and has been eliminated from the event!");

        // Mark player as dead (disconnect counts as death per MVP spec)
        eventManager.markPlayerDead(player);

        // Remove from team if in one
        teamManager.removeFromTeam(player);

        // Check for winner after disconnect
        eventManager.checkForWinner();
    }
}