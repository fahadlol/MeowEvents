package me.oblueberrey.meowMcEvents.listeners;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockListener implements Listener {

    private final EventManager eventManager;

    public BlockListener(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    private void debug(String message) {
        MeowMCEvents plugin = MeowMCEvents.getInstance();
        if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG:BLOCK] " + message);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!eventManager.isEventRunning()) {
            return;
        }

        Player player = event.getPlayer();

        // Block spectators from building
        if (eventManager.isSpectator(player)) {
            event.setCancelled(true);
            return;
        }

        // Only apply to players in the event
        if (!eventManager.isPlayerInEvent(player)) {
            return;
        }

        // If building is not allowed, cancel the event
        if (!eventManager.isBuildingAllowed()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Building is disabled during this event!");
            debug(player.getName() + " tried to place block but building is disabled");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!eventManager.isEventRunning()) {
            return;
        }

        Player player = event.getPlayer();

        // Block spectators from breaking
        if (eventManager.isSpectator(player)) {
            event.setCancelled(true);
            return;
        }

        // Only apply to players in the event
        if (!eventManager.isPlayerInEvent(player)) {
            return;
        }

        // If breaking is not allowed, cancel the event
        if (!eventManager.isBreakingAllowed()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Breaking blocks is disabled during this event!");
            debug(player.getName() + " tried to break block but breaking is disabled");
        }
    }
}