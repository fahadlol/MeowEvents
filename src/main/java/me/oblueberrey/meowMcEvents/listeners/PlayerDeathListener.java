package me.oblueberrey.meowMcEvents.listeners;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.managers.KillStreakManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Fallback death listener for edge cases where FatalDamageListener doesn't catch the death.
 * This should rarely trigger since FatalDamageListener handles most deaths by canceling fatal damage.
 * 
 * Primary purposes:
 * - Clear drops and experience for event players
 * - Handle instant kills that bypass damage events (void, kill commands)
 */
public class PlayerDeathListener implements Listener {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;
    private final KillStreakManager killStreakManager;

    public PlayerDeathListener(MeowMCEvents plugin, EventManager eventManager, KillStreakManager killStreakManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.killStreakManager = killStreakManager;
    }

    private void debug(String message) {
        if (plugin.getConfigManager().shouldLogPlayers()) {
            plugin.getLogger().info("[DEBUG:DEATH] " + message);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!eventManager.isEventRunning()) {
            return;
        }

        Player victim = event.getEntity();

        // Check if victim is in the event (alive or spectator)
        if (!eventManager.isPlayerInEvent(victim) && !eventManager.isSpectator(victim)) {
            return;
        }

        // If player is already a spectator, ignore (they shouldn't die)
        if (eventManager.isSpectator(victim)) {
            debug(victim.getName() + " is spectator and somehow died - clearing drops");
            event.getDrops().clear();
            event.setDroppedExp(0);
            return;
        }

        debug(victim.getName() + " died in event (fallback handler) - this shouldn't normally happen");

        // Clear drops based on config (prevents item duplication exploit)
        if (plugin.getConfigManager().isClearDropsOnDeath()) {
            event.getDrops().clear();
            victim.getInventory().clear();
        }

        // Clear experience based on config
        if (plugin.getConfigManager().isClearExpOnDeath()) {
            event.setDroppedExp(0);
        }

        // Mark for respawn handling - PlayerRespawnListener will handle conversion
        // This is the fallback path for deaths that bypass FatalDamageListener
        if (!eventManager.isPendingRespawn(victim)) {
            eventManager.markPlayerDead(victim);
            eventManager.markPendingRespawn(victim);
            debug(victim.getName() + " marked for pending respawn (fallback path)");
        }
    }
}