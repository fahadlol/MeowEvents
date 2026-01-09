package me.oblueberrey.meowMcEvents.managers;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RankManager {

    private final Map<UUID, Integer> playerRanks;

    public RankManager() {
        this.playerRanks = new HashMap<>();
    }

    /**
     * Increment a player's rank by 1 (called on kill)
     */
    public void incrementRank(Player player) {
        if (player == null) return;

        UUID uuid = player.getUniqueId();
        int currentRank = playerRanks.getOrDefault(uuid, 0);
        playerRanks.put(uuid, currentRank + 1);
    }

    /**
     * Get a player's current rank
     * Returns 0 if player has no rank
     */
    public int getRank(Player player) {
        if (player == null) return 0;
        return playerRanks.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * Reset a player's rank (called on death)
     * Removes entry entirely to save memory
     */
    public void resetRank(Player player) {
        if (player == null) return;
        playerRanks.remove(player.getUniqueId());
    }

    /**
     * Clear all ranks (used when event stops)
     */
    public void clearAllRanks() {
        playerRanks.clear();
    }

    /**
     * Get formatted rank string for display
     * Format: [Rank: X]
     */
    public String getFormattedRank(Player player) {
        int rank = getRank(player);
        return "[Rank: " + rank + "]";
    }

    /**
     * Check if player has any rank data
     */
    public boolean hasRank(UUID uuid) {
        return playerRanks.containsKey(uuid);
    }

    /**
     * Get total number of players with ranks
     */
    public int getTotalRankedPlayers() {
        return playerRanks.size();
    }
}