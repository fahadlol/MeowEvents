package me.oblueberrey.meowMcEvents.listeners;

import me.oblueberrey.meowMcEvents.managers.EventManager;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockListener implements Listener {

    private final EventManager eventManager;

    public BlockListener(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!eventManager.isEventRunning()) {
            return;
        }

        // If building is not allowed, cancel the event
        if (!eventManager.isBuildingAllowed()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Building is disabled during this event!");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!eventManager.isEventRunning()) {
            return;
        }

        // If breaking is not allowed, cancel the event
        if (!eventManager.isBreakingAllowed()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Breaking blocks is disabled during this event!");
        }
    }
}