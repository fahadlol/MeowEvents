package me.oblueberrey.meowMcEvents.listeners;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.DamageTracker;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.managers.KillStreakManager;
import me.oblueberrey.meowMcEvents.utils.LogManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Intercepts fatal damage to prevent actual player death.
 * Instead of dying, players are immediately converted to spectators.
 * This prevents:
 * - Desync issues from death/respawn cycle
 * - Other plugins interfering with respawn handling
 * - Death screen appearing
 * 
 * Uses DamageTracker for proper kill attribution when direct killer isn't available.
 */
public class FatalDamageListener implements Listener {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;
    private final KillStreakManager killStreakManager;

    public FatalDamageListener(MeowMCEvents plugin, EventManager eventManager, KillStreakManager killStreakManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.killStreakManager = killStreakManager;
    }

    private void debug(String message) {
        LogManager log = plugin.getLogManager();
        if (log != null) {
            log.debug(LogManager.Category.SPECTATORS, message);
        }
    }

    /**
     * Intercept damage that would kill the player.
     * Cancel the damage and use EventManager.eliminatePlayer() for clean elimination.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFatalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player victim = (Player) event.getEntity();
        
        // Only handle players in the event (not spectators)
        if (!eventManager.isEventRunning() || !eventManager.isPlayerInEvent(victim)) {
            return;
        }

        // Check if this damage would be fatal
        double finalDamage = event.getFinalDamage();
        double currentHealth = victim.getHealth();
        
        if (finalDamage < currentHealth) {
            return; // Not fatal, let normal damage happen
        }

        // This would kill the player - cancel and convert to spectator
        event.setCancelled(true);
        debug("Intercepted fatal damage for " + victim.getName() + " (damage: " + finalDamage + ", health: " + currentHealth + ")");

        // Try to get killer from the damage event
        Player killer = getKillerFromEvent(event, victim);
        
        // If no direct killer found, check DamageTracker for combat tag
        if (killer == null) {
            DamageTracker damageTracker = plugin.getDamageTracker();
            if (damageTracker != null) {
                killer = damageTracker.getLastAttacker(victim);
                if (killer != null) {
                    String cause = damageTracker.getLastDamageCause(victim);
                    debug("Killer attributed via combat tag: " + killer.getName() + " (" + cause + ")");
                }
            }
        }

        // Clear damage tracking for this player
        DamageTracker damageTracker = plugin.getDamageTracker();
        if (damageTracker != null) {
            damageTracker.clearPlayer(victim.getUniqueId());
        }

        // Use EventManager's eliminatePlayer for centralized elimination logic
        eventManager.eliminatePlayer(victim, killer);
    }

    /**
     * Try to extract the killer player from a damage event.
     * Handles direct damage, projectiles, and other entity sources.
     */
    private Player getKillerFromEvent(EntityDamageEvent event, Player victim) {
        if (!(event instanceof EntityDamageByEntityEvent)) {
            return null;
        }

        EntityDamageByEntityEvent damageByEntity = (EntityDamageByEntityEvent) event;
        Entity damager = damageByEntity.getDamager();

        Player killer = null;

        // Direct player damage
        if (damager instanceof Player) {
            killer = (Player) damager;
        }
        // Projectile damage - get shooter
        else if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player) {
                killer = (Player) shooter;
            }
        }

        // Validate killer is in event and not the victim
        if (killer != null) {
            if (killer.equals(victim) || !eventManager.isPlayerInEvent(killer)) {
                return null;
            }
        }

        return killer;
    }
}
