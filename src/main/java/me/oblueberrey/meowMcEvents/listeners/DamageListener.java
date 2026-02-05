package me.oblueberrey.meowMcEvents.listeners;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

/**
 * Handles damage type blocking based on config options:
 * - Fire damage
 * - Drowning damage
 * - Explosion damage
 * - Fall damage
 * - Hunger
 */
public class DamageListener implements Listener {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;

    public DamageListener(MeowMCEvents plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (!eventManager.isEventRunning()) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Only handle players in the event
        if (!eventManager.isPlayerInEvent(player)) {
            return;
        }

        EntityDamageEvent.DamageCause cause = event.getCause();

        // Check fall damage (also checked in EventManager for immunity period)
        if (cause == EntityDamageEvent.DamageCause.FALL) {
            if (plugin.getConfigManager().isDisableFallDamage() || eventManager.hasFallDamageImmunity(player)) {
                event.setCancelled(true);
                return;
            }
        }

        // Check fire damage
        if (cause == EntityDamageEvent.DamageCause.FIRE ||
            cause == EntityDamageEvent.DamageCause.FIRE_TICK ||
            cause == EntityDamageEvent.DamageCause.LAVA ||
            cause == EntityDamageEvent.DamageCause.HOT_FLOOR) {
            if (plugin.getConfigManager().isDisableFireDamage()) {
                event.setCancelled(true);
                // Also extinguish the player
                player.setFireTicks(0);
                return;
            }
        }

        // Check drowning damage
        if (cause == EntityDamageEvent.DamageCause.DROWNING) {
            if (plugin.getConfigManager().isDisableDrowning()) {
                event.setCancelled(true);
                return;
            }
        }

        // Check explosion damage (keep knockback by only cancelling damage)
        if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
            cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            if (plugin.getConfigManager().isDisableExplosionDamage()) {
                event.setDamage(0);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHungerChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (!eventManager.isEventRunning()) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Only handle players in the event
        if (!eventManager.isPlayerInEvent(player)) {
            return;
        }

        // Disable hunger if configured
        if (plugin.getConfigManager().isDisableHunger()) {
            // Only block hunger decrease, allow eating to increase
            if (event.getFoodLevel() < player.getFoodLevel()) {
                event.setCancelled(true);
            }
        }
    }
}
