package me.oblueberrey.meowMcEvents.listeners;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.listeners.SpectatorCompassListener;
import me.oblueberrey.meowMcEvents.utils.ConfigManager;
import me.oblueberrey.meowMcEvents.utils.LogManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerRespawnListener implements Listener {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;

    public PlayerRespawnListener(MeowMCEvents plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        LogManager log = plugin.getLogManager();

        // Check if this player died during an event and is pending respawn
        if (eventManager.isPendingRespawn(player)) {
            if (log != null) log.info(LogManager.Category.SPECTATORS, "Player " + player.getName() + " respawning after event death - converting to spectator");

            // Clear the pending respawn flag FIRST to prevent double processing
            eventManager.clearPendingRespawn(player);

            // Capture event state NOW to check later
            final boolean eventWasRunning = eventManager.isEventRunning();
            
            // Set respawn to event spawn (spectators stay in arena) only if event still running
            Location eventSpawn = plugin.getConfigManager().getSpawnLocation();
            Location playerSpawn = plugin.getConfigManager().getPlayerSpawnLocation();
            
            if (eventWasRunning && eventSpawn != null && eventSpawn.getWorld() != null) {
                event.setRespawnLocation(eventSpawn);
                if (log != null) log.debug(LogManager.Category.SPECTATORS, "Set respawn location to event spawn for " + player.getName());
            } else if (playerSpawn != null && playerSpawn.getWorld() != null) {
                // Event ended or no event spawn - send to player spawn instead
                event.setRespawnLocation(playerSpawn);
                if (log != null) log.debug(LogManager.Category.SPECTATORS, "Event not running, set respawn to player spawn for " + player.getName());
            } else {
                if (log != null) log.error(LogManager.Category.ERRORS, "Both spawns are NULL - Cannot set respawn location for " + player.getName());
            }

            // Turn into spectator on next tick (after respawn completes)
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) {
                    if (log != null) log.warn(LogManager.Category.SPECTATORS, "Player " + player.getName() + " went offline before spectator conversion");
                    return;
                }

                // Check if player is already a spectator (prevent double conversion)
                if (eventManager.isSpectator(player)) {
                    if (log != null) log.debug(LogManager.Category.SPECTATORS, "Player " + player.getName() + " is already a spectator - skipping conversion");
                    return;
                }

                // If event ended between death and respawn, reset player state properly
                if (!eventManager.isEventRunning()) {
                    if (log != null) log.warn(LogManager.Category.SPECTATORS, "Event ended before " + player.getName() + " could become spectator - resetting state");
                    
                    // Reset player to clean state
                    player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    player.setFlySpeed(0.1f);
                    player.getInventory().clear();
                    
                    // Teleport to player spawn if available
                    Location safeSpawn = plugin.getConfigManager().getPlayerSpawnLocation();
                    if (safeSpawn != null && safeSpawn.getWorld() != null) {
                        player.teleport(safeSpawn);
                    }
                    
                    player.sendMessage(ConfigManager.colorize(
                            "&#FF9944The event has ended. &#AAAAAAYou have been returned to spawn."));
                    return;
                }

                try {
                    // Add as spectator directly (don't use command since it checks if already spectator)
                    eventManager.addSpectator(player);
                    if (log != null) log.info(LogManager.Category.SPECTATORS, "Added " + player.getName() + " as spectator successfully");

                    // Teleport to event spawn
                    Location spawn = plugin.getConfigManager().getSpawnLocation();
                    if (spawn != null && spawn.getWorld() != null) {
                        player.teleport(spawn);
                        if (log != null) log.debug(LogManager.Category.SPECTATORS, "Teleported " + player.getName() + " to event spawn");
                    } else {
                        if (log != null) log.error(LogManager.Category.ERRORS, "FAILED to teleport " + player.getName() + " - event spawn is null");
                    }

                    player.sendMessage(ConfigManager.colorize(
                            "&#FF9944You were eliminated! &#AAAAAAYou are now spectating."));

                    // Ensure spectator status persists for 3 seconds (60 ticks)
                    // Check every 10 ticks (0.5 seconds) for 3 seconds total
                    for (int i = 1; i <= 6; i++) {
                        final int checkNum = i;
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            // Skip if event ended or player offline
                            if (!eventManager.isEventRunning() || !player.isOnline()) {
                                return;
                            }
                            
                            // Ensure player is still a spectator with correct items
                            if (eventManager.isSpectator(player)) {
                                ensureSpectatorState(player, log, checkNum);
                            }
                        }, 10L * i); // 10, 20, 30, 40, 50, 60 ticks
                    }

                } catch (Exception e) {
                    if (log != null) log.error(LogManager.Category.ERRORS, "Exception converting " + player.getName() + " to spectator: " + e.getMessage(), e);
                    
                    // Recovery: reset player to clean state on error
                    try {
                        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                        player.setAllowFlight(false);
                        player.setFlying(false);
                        player.getInventory().clear();
                    } catch (Exception recovery) {
                        if (log != null) log.error(LogManager.Category.ERRORS, "Recovery also failed for " + player.getName() + ": " + recovery.getMessage(), recovery);
                    }
                }
            });
        }
    }

    /**
     * Ensure player has correct spectator state and items
     */
    private void ensureSpectatorState(Player player, LogManager log, int checkNum) {
        String gamemodeConfig = plugin.getConfigManager().getSpectatorGamemode();
        
        // Ensure correct gamemode
        if ("SPECTATOR".equalsIgnoreCase(gamemodeConfig)) {
            if (player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                if (log != null) log.debug(LogManager.Category.SPECTATORS, 
                    "Check " + checkNum + ": Fixed gamemode for " + player.getName());
            }
        } else {
            // Adventure mode with flying
            if (player.getGameMode() != org.bukkit.GameMode.ADVENTURE) {
                player.setGameMode(org.bukkit.GameMode.ADVENTURE);
                if (log != null) log.debug(LogManager.Category.SPECTATORS, 
                    "Check " + checkNum + ": Fixed gamemode to ADVENTURE for " + player.getName());
            }
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
            }
            if (!player.isFlying()) {
                player.setFlying(true);
            }
            
            // Ensure spectator items exist (only for adventure mode)
            int compassSlot = plugin.getConfigManager().getCompassSlot();
            int leaveSlot = plugin.getConfigManager().getLeaveSlot();
            
            if (player.getInventory().getItem(compassSlot) == null) {
                player.getInventory().setItem(compassSlot, SpectatorCompassListener.createSpectatorCompass());
                if (log != null) log.debug(LogManager.Category.SPECTATORS, 
                    "Check " + checkNum + ": Restored compass for " + player.getName());
            }
            if (player.getInventory().getItem(leaveSlot) == null) {
                player.getInventory().setItem(leaveSlot, SpectatorCompassListener.createLeaveDye());
                if (log != null) log.debug(LogManager.Category.SPECTATORS, 
                    "Check " + checkNum + ": Restored leave dye for " + player.getName());
            }
        }
        
        // Ensure spectator is hidden from non-spectators
        for (org.bukkit.entity.Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(player)) {
                if (eventManager.isSpectator(onlinePlayer)) {
                    // Other spectators can see this spectator
                    onlinePlayer.showPlayer(plugin, player);
                    player.showPlayer(plugin, onlinePlayer);
                } else {
                    // Non-spectators cannot see this spectator
                    onlinePlayer.hidePlayer(plugin, player);
                }
            }
        }
    }
}
