package me.oblueberrey.meowMcEvents.listeners;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class RegenListener implements Listener {

    private final EventManager eventManager;

    public RegenListener(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    private void debug(String message) {
        MeowMCEvents plugin = MeowMCEvents.getInstance();
        if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG:REGEN] " + message);
        }
    }

    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        // Only handle player regen
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        // Only handle during event
        if (!eventManager.isEventRunning()) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Only apply to players in the event
        if (!eventManager.isPlayerInEvent(player)) {
            return;
        }

        // Check if this is natural regeneration (from saturation/hunger)
        EntityRegainHealthEvent.RegainReason reason = event.getRegainReason();
        if (reason == EntityRegainHealthEvent.RegainReason.SATIATED ||
            reason == EntityRegainHealthEvent.RegainReason.REGEN) {

            // If natural regen is not allowed, cancel it
            if (!eventManager.isNaturalRegenAllowed()) {
                event.setCancelled(true);
                debug(player.getName() + " natural regen blocked (reason: " + reason + ")");
            }
        }
    }
}
