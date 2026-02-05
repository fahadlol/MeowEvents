package me.oblueberrey.meowMcEvents.managers;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KillStreakManager {

    // Thread-safe map to prevent ConcurrentModificationException
    private final Map<UUID, Integer> playerStreaks;

    public KillStreakManager() {
        this.playerStreaks = new ConcurrentHashMap<>();
    }

    /**
     * Increment a player's streak by 1 (called on kill)
     */
    public void incrementStreak(Player player) {
        if (player == null) return;

        UUID uuid = player.getUniqueId();
        int currentStreak = playerStreaks.getOrDefault(uuid, 0);
        playerStreaks.put(uuid, currentStreak + 1);
    }

    /**
     * Get a player's current streak
     * Returns 0 if player has no streak
     */
    public int getStreak(Player player) {
        if (player == null) return 0;
        return playerStreaks.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * Reset a player's streak (called on death)
     * Removes entry entirely to save memory
     */
    public void resetStreak(Player player) {
        if (player == null) return;
        playerStreaks.remove(player.getUniqueId());
    }

    /**
     * Clear all streaks (used when event stops)
     */
    public void clearAllStreaks() {
        playerStreaks.clear();
    }

    /**
     * Get formatted streak string for display
     * Format: [Streak: X]
     */
    public String getFormattedStreak(Player player) {
        int streak = getStreak(player);
        return "[Streak: " + streak + "]";
    }

    /**
     * Check if player has any streak data
     */
    public boolean hasStreak(UUID uuid) {
        return playerStreaks.containsKey(uuid);
    }

    /**
     * Get total number of players with streaks
     */
    public int getTotalTrackedPlayers() {
        return playerStreaks.size();
    }
}