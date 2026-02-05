package me.oblueberrey.meowMcEvents.listeners;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.utils.LogManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles void death protection and /kill command interception.
 * 
 * Void Protection:
 * - Catches players falling below Y=-64 (void)
 * - Eliminates them before they die naturally
 * 
 * Command Interception:
 * - Intercepts /kill, /suicide commands
 * - Uses eliminatePlayer() instead of letting them die normally
 */
public class VoidAndCommandListener implements Listener {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;
    
    // Track players being processed to prevent double elimination
    private final Set<UUID> beingProcessed = ConcurrentHashMap.newKeySet();
    
    // Void threshold (Y level below which players are eliminated)
    private static final int VOID_THRESHOLD = -64;

    public VoidAndCommandListener(MeowMCEvents plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    private void debug(String message) {
        LogManager log = plugin.getLogManager();
        if (log != null) {
            log.debug(LogManager.Category.PLAYERS, message);
        }
    }

    // ==================== VOID PROTECTION ====================

    /**
     * Catch players falling into the void and eliminate them before natural death.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only check Y level changes for performance
        if (event.getFrom().getBlockY() == event.getTo().getBlockY()) {
            return;
        }

        Player player = event.getPlayer();
        Location to = event.getTo();

        // Check if below void threshold
        if (to.getY() > VOID_THRESHOLD) {
            return;
        }

        // Only handle players in the event
        if (!eventManager.isEventRunning() || !eventManager.isPlayerInEvent(player)) {
            return;
        }

        // Prevent double processing
        if (!beingProcessed.add(player.getUniqueId())) {
            return;
        }

        try {
            debug("Player " + player.getName() + " fell into void at Y=" + to.getY());

            // Cancel movement to prevent further void damage
            event.setCancelled(true);

            // Check if someone knocked them into the void (combat tag)
            Player killer = null;
            me.oblueberrey.meowMcEvents.managers.DamageTracker damageTracker = plugin.getDamageTracker();
            if (damageTracker != null) {
                killer = damageTracker.getLastAttacker(player);
                if (killer != null) {
                    debug("Void death attributed to " + killer.getName() + " via combat tag");
                }
            }

            // Teleport to event spawn first to prevent void death
            Location eventSpawn = plugin.getConfigManager().getSpawnLocation();
            if (eventSpawn != null && eventSpawn.getWorld() != null) {
                player.teleport(eventSpawn);
            }

            // Eliminate the player with killer attribution if available
            eventManager.eliminatePlayer(player, killer);
            
        } finally {
            // Clear processing flag after a short delay
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                beingProcessed.remove(player.getUniqueId());
            }, 20L);
        }
    }

    // ==================== COMMAND INTERCEPTION ====================

    /**
     * Intercept /kill and /suicide commands for event players.
     * Uses eliminatePlayer() instead of letting them die normally.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase().trim();

        // Check if it's a kill/suicide command
        if (!isKillCommand(command)) {
            return;
        }

        // Only handle players in the event
        if (!eventManager.isEventRunning() || !eventManager.isPlayerInEvent(player)) {
            return;
        }

        // Prevent double processing
        if (beingProcessed.contains(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        debug("Intercepted kill command from " + player.getName() + ": " + command);

        // Cancel the command
        event.setCancelled(true);

        // Add to processing set
        beingProcessed.add(player.getUniqueId());

        try {
            // Eliminate the player properly
            eventManager.eliminatePlayer(player, null);
            player.sendMessage(me.oblueberrey.meowMcEvents.utils.ConfigManager.colorize(
                    "&#AAAAAAYou have been eliminated from the event."));
        } finally {
            // Clear processing flag after a short delay
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                beingProcessed.remove(player.getUniqueId());
            }, 20L);
        }
    }

    /**
     * Check if the command is a kill/suicide command.
     */
    private boolean isKillCommand(String command) {
        // Remove leading slash if present
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        
        // Check for common kill commands (with or without arguments)
        return command.equals("kill") ||
               command.startsWith("kill ") ||
               command.equals("suicide") ||
               command.startsWith("suicide ") ||
               command.equals("kys") ||
               command.startsWith("kys ");
    }

    /**
     * Clear processing state for a player (called on quit).
     */
    public void clearPlayer(UUID uuid) {
        beingProcessed.remove(uuid);
    }
}
