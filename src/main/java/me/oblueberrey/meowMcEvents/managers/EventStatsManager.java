package me.oblueberrey.meowMcEvents.managers;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks event statistics including placements and kills
 */
public class EventStatsManager {

    private final MeowMCEvents plugin;

    // Thread-safe death order tracking - first UUID in list died first (last place)
    private final List<UUID> deathOrder = new CopyOnWriteArrayList<>();

    // Thread-safe kill tracking - UUID -> kill count
    private final Map<UUID, Integer> killCounts = new ConcurrentHashMap<>();

    // Thread-safe player names cache (in case they disconnect)
    private final Map<UUID, String> playerNames = new ConcurrentHashMap<>();

    // Total players who started the event (volatile for visibility)
    private volatile int totalParticipants = 0;

    public EventStatsManager(MeowMCEvents plugin) {
        this.plugin = plugin;
    }

    /**
     * Reset all stats for a new event
     */
    public void reset() {
        deathOrder.clear();
        killCounts.clear();
        playerNames.clear();
        totalParticipants = 0;

        if (plugin.getConfigManager().shouldLogEvents()) {
            plugin.getLogger().info("[DEBUG:STATS] Event stats reset");
        }
    }

    /**
     * Register all participants at event start
     */
    public void registerParticipants(Set<UUID> participants) {
        totalParticipants = participants.size();
        for (UUID uuid : participants) {
            killCounts.put(uuid, 0);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                playerNames.put(uuid, player.getName());
            }
        }

        if (plugin.getConfigManager().shouldLogEvents()) {
            plugin.getLogger().info("[DEBUG:STATS] Registered " + totalParticipants + " participants");
        }
    }

    /**
     * Record a player death (for placement tracking)
     */
    public void recordDeath(UUID uuid) {
        if (!deathOrder.contains(uuid)) {
            deathOrder.add(uuid);

            // Cache name
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                playerNames.put(uuid, player.getName());
            }

            if (plugin.getConfigManager().shouldLogEvents()) {
                plugin.getLogger().info("[DEBUG:STATS] Recorded death for " + getPlayerName(uuid) +
                        " - Placement: #" + getPlacement(uuid));
            }
        }
    }

    /**
     * Record a kill
     */
    public void recordKill(UUID killerUuid) {
        killCounts.merge(killerUuid, 1, Integer::sum);

        // Cache name
        Player player = Bukkit.getPlayer(killerUuid);
        if (player != null) {
            playerNames.put(killerUuid, player.getName());
        }

        if (plugin.getConfigManager().shouldLogEvents()) {
            plugin.getLogger().info("[DEBUG:STATS] " + getPlayerName(killerUuid) +
                    " now has " + killCounts.get(killerUuid) + " kills");
        }
    }

    /**
     * Get placement for a player (1 = winner, higher = worse)
     * Players who died first get higher (worse) placement numbers
     */
    public int getPlacement(UUID uuid) {
        int deathIndex = deathOrder.indexOf(uuid);
        if (deathIndex == -1) {
            // Player is still alive or is the winner
            return 1;
        }
        // Convert death order to placement
        // First to die = last place = totalParticipants
        // Second to die = second to last = totalParticipants - 1
        return totalParticipants - deathIndex;
    }

    /**
     * Get kill count for a player
     */
    public int getKills(UUID uuid) {
        return killCounts.getOrDefault(uuid, 0);
    }

    /**
     * Get player name from cache or Bukkit
     */
    public String getPlayerName(UUID uuid) {
        if (playerNames.containsKey(uuid)) {
            return playerNames.get(uuid);
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }
        return "Unknown";
    }

    /**
     * Get player with most kills
     */
    public UUID getMostKillsPlayer() {
        return killCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Get top N players by kills
     */
    public List<Map.Entry<UUID, Integer>> getTopKillers(int n) {
        return killCounts.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(n)
                .toList();
    }

    /**
     * Get placements in order (1st, 2nd, 3rd, etc.)
     */
    public List<UUID> getPlacementsInOrder() {
        List<UUID> placements = new ArrayList<>();

        // Winner(s) first - anyone not in deathOrder
        Set<UUID> allParticipants = new HashSet<>(killCounts.keySet());
        for (UUID uuid : allParticipants) {
            if (!deathOrder.contains(uuid)) {
                placements.add(uuid);
            }
        }

        // Then add dead players in reverse order (last to die = 2nd place, etc.)
        for (int i = deathOrder.size() - 1; i >= 0; i--) {
            placements.add(deathOrder.get(i));
        }

        return placements;
    }

    /**
     * Announce final rankings at event end
     * Uses styled RGB colors and emojis (grey emojis, yellow/orange text)
     */
    public void announceRankings(UUID winnerUuid, boolean isTeamMode, int winningTeam, TeamManager teamManager) {
        // Styled colors
        String grey = "&8";
        String yellow = "&e";
        String orange = "&6";
        String red = "&c";
        String white = "&f";

        // Key symbols
        String star = "★";
        String skull = "☠";
        
        String title = MessageUtils.colorize("&6&lFINAL STANDINGS");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(colorize(MessageUtils.center("&8★ " + title + " &8★")));

        // Announce placements
        List<UUID> placements = getPlacementsInOrder();
        int displayCount = Math.min(placements.size(), 5); // Show top 5

        for (int i = 0; i < displayCount; i++) {
            UUID uuid = placements.get(i);
            String name = getPlayerName(uuid);
            int kills = getKills(uuid);

            String prefix;
            String color;
            switch (i) {
                case 0:
                    prefix = yellow + star + " 1st ";
                    color = yellow;
                    break;
                case 1:
                    prefix = white + "  2nd ";
                    color = white;
                    break;
                case 2:
                    prefix = orange + "  3rd ";
                    color = orange;
                    break;
                default:
                    prefix = grey + "  " + (i + 1) + "th ";
                    color = grey;
                    break;
            }

            String teamInfo = "";
            if (isTeamMode && teamManager != null) {
                int team = teamManager.getTeam(uuid);
                if (team != -1) {
                    teamInfo = " " + grey + "(Team " + team + ")";
                }
            }

            String killWord = (kills == 1) ? "kill" : "kills";
            Bukkit.broadcastMessage(colorize(MessageUtils.center(prefix + color + name + grey + " - " +
                    red + kills + " " + killWord + teamInfo)));
        }

        // Announce most kills if different from winner
        UUID mostKillsPlayer = getMostKillsPlayer();
        int mostKills = mostKillsPlayer != null ? getKills(mostKillsPlayer) : 0;

        if (mostKillsPlayer != null && mostKills > 0) {
            String mostKillsName = getPlayerName(mostKillsPlayer);
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage(colorize(MessageUtils.center(grey + skull + " " + red + "Most Kills &f" + mostKillsName + " " + grey + "(" + red + mostKills + grey + ")")));
        }

        Bukkit.broadcastMessage(colorize(MessageUtils.center("&7Total Participants: &f" + totalParticipants)));
        Bukkit.broadcastMessage("");

        // Play sound to all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    /**
     * Translate hex color codes (&#RRGGBB) to chat colors
     */
    private String colorize(String message) {
        return MessageUtils.colorize(message);
    }

    /**
     * Get total participants
     */
    public int getTotalParticipants() {
        return totalParticipants;
    }
}
