package me.oblueberrey.meowMcEvents.managers;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks damage sources for proper kill attribution.
 * Handles cases where the direct damager is not the actual attacker:
 * - Projectiles (arrows, tridents, snowballs)
 * - Explosions (TNT, creepers, beds)
 * - Fire/Lava (player pushed into fire/lava)
 * - Falling (player knocked off edge)
 * 
 * Combat tag expires after a configurable time (default 8 seconds).
 */
public class DamageTracker {

    private final MeowMCEvents plugin;
    
    // Maps victim UUID -> attacker UUID (last player who damaged them)
    private final Map<UUID, UUID> lastDamager = new ConcurrentHashMap<>();
    
    // Maps victim UUID -> timestamp of last damage
    private final Map<UUID, Long> lastDamageTime = new ConcurrentHashMap<>();
    
    // Maps victim UUID -> damage cause description (for kill feed)
    private final Map<UUID, String> lastDamageCause = new ConcurrentHashMap<>();
    
    // Combat tag duration in milliseconds (default 8 seconds)
    private static final long COMBAT_TAG_DURATION_MS = 8000;
    
    // Cleanup task
    private BukkitTask cleanupTask;

    public DamageTracker(MeowMCEvents plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    /**
     * Record damage from one player to another.
     * 
     * @param victim The player who received damage
     * @param attacker The player who dealt the damage
     * @param cause Description of the damage cause (e.g., "arrow", "TNT", "pushed")
     */
    public void recordDamage(Player victim, Player attacker, String cause) {
        if (victim == null || attacker == null) return;
        if (victim.equals(attacker)) return; // Ignore self-damage
        
        UUID victimId = victim.getUniqueId();
        UUID attackerId = attacker.getUniqueId();
        
        lastDamager.put(victimId, attackerId);
        lastDamageTime.put(victimId, System.currentTimeMillis());
        lastDamageCause.put(victimId, cause);
        
        debug("Recorded damage: " + attacker.getName() + " -> " + victim.getName() + " (" + cause + ")");
    }

    /**
     * Get the last player who damaged this victim (within combat tag duration).
     * Returns null if no valid attacker or combat tag expired.
     */
    public Player getLastAttacker(Player victim) {
        if (victim == null) return null;
        
        UUID victimId = victim.getUniqueId();
        UUID attackerId = lastDamager.get(victimId);
        
        if (attackerId == null) return null;
        
        // Check if combat tag expired
        Long damageTime = lastDamageTime.get(victimId);
        if (damageTime == null) return null;
        
        long elapsed = System.currentTimeMillis() - damageTime;
        if (elapsed > COMBAT_TAG_DURATION_MS) {
            // Combat tag expired, clear data
            clearPlayer(victimId);
            return null;
        }
        
        return Bukkit.getPlayer(attackerId);
    }

    /**
     * Get the cause of the last damage dealt to this victim.
     */
    public String getLastDamageCause(Player victim) {
        if (victim == null) return null;
        return lastDamageCause.get(victim.getUniqueId());
    }

    /**
     * Check if a player has an active combat tag.
     */
    public boolean hasCombatTag(Player player) {
        return getLastAttacker(player) != null;
    }

    /**
     * Clear tracking data for a player (on death, quit, etc.)
     */
    public void clearPlayer(UUID uuid) {
        lastDamager.remove(uuid);
        lastDamageTime.remove(uuid);
        lastDamageCause.remove(uuid);
    }

    /**
     * Clear all tracking data (on event end).
     */
    public void clearAll() {
        lastDamager.clear();
        lastDamageTime.clear();
        lastDamageCause.clear();
    }

    /**
     * Start periodic cleanup of expired combat tags.
     */
    private void startCleanupTask() {
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            
            // Remove expired entries
            lastDamageTime.entrySet().removeIf(entry -> {
                if (now - entry.getValue() > COMBAT_TAG_DURATION_MS) {
                    UUID victimId = entry.getKey();
                    lastDamager.remove(victimId);
                    lastDamageCause.remove(victimId);
                    return true;
                }
                return false;
            });
        }, 200L, 200L); // Run every 10 seconds
    }

    /**
     * Stop the cleanup task (on plugin disable).
     */
    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        clearAll();
    }

    private void debug(String message) {
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG:DAMAGE_TRACKER] " + message);
        }
    }
}
