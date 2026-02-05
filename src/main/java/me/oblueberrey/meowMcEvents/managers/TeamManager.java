package me.oblueberrey.meowMcEvents.managers;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeamManager {

    // Thread-safe collections to prevent ConcurrentModificationException
    private final Map<Integer, Set<UUID>> teams; // Team number -> Set of player UUIDs
    private final Map<UUID, Integer> playerTeams; // Player UUID -> Team number

    // Lock for compound operations that need atomicity
    private final Object teamLock = new Object();

    public TeamManager() {
        this.teams = new ConcurrentHashMap<>();
        this.playerTeams = new ConcurrentHashMap<>();
    }

    private void debug(String message) {
        MeowMCEvents plugin = MeowMCEvents.getInstance();
        if (plugin != null && plugin.getConfigManager().shouldLogTeams()) {
            plugin.getLogger().info("[DEBUG:TEAM] " + message);
        }
    }

    /**
     * Assign players to teams automatically
     * Shuffles players for random assignment
     * Handles uneven player counts (remainder goes to smaller teams)
     */
    public void assignTeams(List<Player> players, int teamSize) {
        synchronized (teamLock) {
            clearTeamsInternal();

            if (players == null || players.isEmpty() || teamSize < 1) {
                debug("assignTeams called with invalid params: players=" + (players == null ? "null" : players.size()) + ", teamSize=" + teamSize);
                return;
            }

            debug("Assigning " + players.size() + " players to teams of size " + teamSize);

            // Shuffle players for random assignment
            List<Player> shuffled = new ArrayList<>(players);
            Collections.shuffle(shuffled);

            int teamNumber = 1;
            int playersInCurrentTeam = 0;

            for (Player player : shuffled) {
                if (player == null) continue;

                // Get or create team set - use thread-safe set
                Set<UUID> team = teams.computeIfAbsent(teamNumber, k -> ConcurrentHashMap.newKeySet());

                // Add player to team
                UUID uuid = player.getUniqueId();
                team.add(uuid);
                playerTeams.put(uuid, teamNumber);

                debug("Assigned " + player.getName() + " to Team " + teamNumber);

                playersInCurrentTeam++;

                // Move to next team if current team is full
                if (playersInCurrentTeam >= teamSize) {
                    teamNumber++;
                    playersInCurrentTeam = 0;
                }
            }

            debug("Team assignment complete. Total teams: " + teams.size());
        }
    }

    /**
     * Get team number for a player
     * Returns -1 if player is not in a team
     */
    public int getTeam(Player player) {
        if (player == null) return -1;
        return playerTeams.getOrDefault(player.getUniqueId(), -1);
    }

    /**
     * Get team number by UUID
     */
    public int getTeam(UUID uuid) {
        if (uuid == null) return -1;
        return playerTeams.getOrDefault(uuid, -1);
    }

    /**
     * Check if two players are on the same team
     */
    public boolean isSameTeam(Player player1, Player player2) {
        if (player1 == null || player2 == null) return false;

        int team1 = getTeam(player1);
        int team2 = getTeam(player2);
        return team1 != -1 && team1 == team2;
    }

    /**
     * Get all players in a specific team
     */
    public Set<UUID> getTeamMembers(int teamNumber) {
        return teams.getOrDefault(teamNumber, new HashSet<>());
    }

    /**
     * Get total number of teams
     */
    public int getTeamCount() {
        return teams.size();
    }

    /**
     * Get the count of alive team members for a specific team
     */
    public int getAliveTeamMemberCount(int teamNumber) {
        MeowMCEvents plugin = MeowMCEvents.getInstance();
        if (plugin == null || plugin.getEventManager() == null) return 0;

        Set<UUID> teamMembers = getTeamMembers(teamNumber);
        Set<UUID> alivePlayers = plugin.getEventManager().getAlivePlayers();

        int count = 0;
        for (UUID uuid : teamMembers) {
            if (alivePlayers.contains(uuid)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Remove player from their team
     * Used when player disconnects or dies
     */
    public void removeFromTeam(Player player) {
        if (player == null) return;
        removeFromTeam(player.getUniqueId());
    }

    /**
     * Remove player from team by UUID
     */
    public void removeFromTeam(UUID uuid) {
        if (uuid == null) return;

        synchronized (teamLock) {
            Integer teamNumber = playerTeams.remove(uuid);

            if (teamNumber != null) {
                Set<UUID> team = teams.get(teamNumber);
                if (team != null) {
                    team.remove(uuid);
                    debug("Removed player from Team " + teamNumber + ". Team size now: " + team.size());

                    // Remove empty teams
                    if (team.isEmpty()) {
                        teams.remove(teamNumber);
                        debug("Team " + teamNumber + " is now empty and removed");
                    }
                }
            }
        }
    }

    /**
     * Check if a team has at least one alive player
     */
    public boolean isTeamAlive(int teamNumber, Set<UUID> alivePlayers) {
        if (alivePlayers == null) return false;

        Set<UUID> teamMembers = getTeamMembers(teamNumber);

        for (UUID member : teamMembers) {
            if (alivePlayers.contains(member)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get number of alive teams
     */
    public int getAliveTeamCount(Set<UUID> alivePlayers) {
        if (alivePlayers == null) return 0;

        int aliveTeams = 0;

        for (int teamNumber : teams.keySet()) {
            if (isTeamAlive(teamNumber, alivePlayers)) {
                aliveTeams++;
            }
        }

        return aliveTeams;
    }

    /**
     * Get the team number of the last alive team
     * Returns -1 if no team is alive
     */
    public int getWinningTeam(Set<UUID> alivePlayers) {
        if (alivePlayers == null) return -1;

        for (int teamNumber : teams.keySet()) {
            if (isTeamAlive(teamNumber, alivePlayers)) {
                return teamNumber;
            }
        }
        return -1;
    }

    /**
     * Get all team numbers that currently exist
     */
    public Set<Integer> getAllTeamNumbers() {
        return new HashSet<>(teams.keySet());
    }

    /**
     * Get team color for display
     * Cycles through 16 colors to support more teams
     */
    public ChatColor getTeamColor(int teamNumber) {
        ChatColor[] colors = {
                ChatColor.RED,
                ChatColor.BLUE,
                ChatColor.GREEN,
                ChatColor.YELLOW,
                ChatColor.AQUA,
                ChatColor.LIGHT_PURPLE,
                ChatColor.GOLD,
                ChatColor.WHITE,
                ChatColor.DARK_RED,
                ChatColor.DARK_BLUE,
                ChatColor.DARK_GREEN,
                ChatColor.DARK_AQUA,
                ChatColor.DARK_PURPLE,
                ChatColor.GRAY,
                ChatColor.DARK_GRAY,
                ChatColor.BLACK
        };

        // Maximum 8 teams supported (for up to 8 players in solo or 4v4 team modes)
        int maxTeams = 8;

        // Return gray for invalid team numbers (less than 1 or greater than max)
        if (teamNumber < 1 || teamNumber > maxTeams) {
            return ChatColor.GRAY;
        }

        return colors[teamNumber - 1];
    }

    /**
     * Get formatted team name with color
     */
    public String getFormattedTeamName(int teamNumber) {
        ChatColor color = getTeamColor(teamNumber);
        return color + "Team " + teamNumber;
    }

    /**
     * Clear all team data
     * Called when event stops
     */
    public void clearTeams() {
        synchronized (teamLock) {
            clearTeamsInternal();
        }
    }

    /**
     * Internal clear without lock - called from synchronized methods
     */
    private void clearTeamsInternal() {
        int teamCount = teams.size();
        int playerCount = playerTeams.size();
        teams.clear();
        playerTeams.clear();
        debug("Cleared all teams. Removed " + teamCount + " teams and " + playerCount + " player assignments");
    }

    /**
     * Check if team mode is active
     * Returns true if teams have been assigned
     */
    public boolean isTeamMode() {
        return !teams.isEmpty();
    }

    /**
     * Get total number of players in all teams
     */
    public int getTotalPlayers() {
        return playerTeams.size();
    }

    /**
     * Auto-balance teams by moving players from larger teams to smaller teams
     * Called when a player leaves to keep teams balanced
     * @param alivePlayers Set of alive player UUIDs to consider for balancing
     * @return true if any rebalancing occurred
     */
    public boolean autoBalanceTeams(Set<UUID> alivePlayers) {
        synchronized (teamLock) {
            if (!isTeamMode() || teams.size() < 2) {
                debug("Auto-balance skipped: not in team mode or less than 2 teams");
                return false;
            }

            // Calculate alive players per team
            Map<Integer, List<UUID>> alivePerTeam = new HashMap<>();
            for (int teamNum : teams.keySet()) {
                alivePerTeam.put(teamNum, new ArrayList<>());
                Set<UUID> teamMembers = teams.get(teamNum);
                if (teamMembers != null) {
                    for (UUID member : teamMembers) {
                        if (alivePlayers.contains(member)) {
                            alivePerTeam.get(teamNum).add(member);
                        }
                    }
                }
            }

            // Remove empty teams from consideration
            alivePerTeam.entrySet().removeIf(entry -> entry.getValue().isEmpty());

            if (alivePerTeam.size() < 2) {
                debug("Auto-balance skipped: less than 2 teams with alive players");
                return false;
            }

            boolean balanced = false;
            int iterations = 0;
            int maxIterations = 10; // Prevent infinite loops

            while (iterations < maxIterations) {
                iterations++;

                // Find team with most and least alive players
                int maxTeam = -1, minTeam = -1;
                int maxSize = Integer.MIN_VALUE, minSize = Integer.MAX_VALUE;

                for (Map.Entry<Integer, List<UUID>> entry : alivePerTeam.entrySet()) {
                    int size = entry.getValue().size();
                    if (size > maxSize) {
                        maxSize = size;
                        maxTeam = entry.getKey();
                    }
                    if (size < minSize) {
                        minSize = size;
                        minTeam = entry.getKey();
                    }
                }

                // Only balance if difference is 2 or more
                if (maxSize - minSize < 2) {
                    break;
                }

                // Move one player from largest team to smallest team
                List<UUID> largeTeamPlayers = alivePerTeam.get(maxTeam);
                if (largeTeamPlayers.isEmpty()) break;

                UUID playerToMove = largeTeamPlayers.get(largeTeamPlayers.size() - 1); // Move last player
                movePlayerToTeamInternal(playerToMove, minTeam);

                // Update local tracking
                largeTeamPlayers.remove(playerToMove);
                alivePerTeam.get(minTeam).add(playerToMove);

                // Notify the moved player
                Player player = org.bukkit.Bukkit.getPlayer(playerToMove);
                if (player != null && player.isOnline()) {
                    String msg = me.oblueberrey.meowMcEvents.utils.ConfigManager.colorize(
                            "&#AAAAAA-&#FF9944auto-balance &#AAAAAA-&#FFE566team " + minTeam);
                    player.sendMessage(msg);
                }

                debug("Auto-balanced: moved player to Team " + minTeam + " (was " + maxSize + " vs " + minSize + ")");
                balanced = true;
            }

            if (balanced) {
                org.bukkit.Bukkit.broadcastMessage(me.oblueberrey.meowMcEvents.utils.ConfigManager.colorize(
                        "&#AAAAAA&#FF9944teams auto-balanced"));
            }

            return balanced;
        }
    }

    /**
     * Internal move without lock - called from synchronized methods
     */
    private void movePlayerToTeamInternal(UUID uuid, int newTeam) {
        if (uuid == null) return;

        // Remove from old team
        Integer oldTeam = playerTeams.get(uuid);
        if (oldTeam != null) {
            Set<UUID> oldTeamSet = teams.get(oldTeam);
            if (oldTeamSet != null) {
                oldTeamSet.remove(uuid);
                if (oldTeamSet.isEmpty()) {
                    teams.remove(oldTeam);
                }
            }
        }

        // Add to new team - use thread-safe set
        Set<UUID> newTeamSet = teams.computeIfAbsent(newTeam, k -> ConcurrentHashMap.newKeySet());
        newTeamSet.add(uuid);
        playerTeams.put(uuid, newTeam);

        debug("Moved player from Team " + oldTeam + " to Team " + newTeam);
    }

    /**
     * Move a player to a different team
     * @param uuid Player UUID
     * @param newTeam New team number
     */
    public void movePlayerToTeam(UUID uuid, int newTeam) {
        if (uuid == null) return;

        synchronized (teamLock) {
            // Remove from old team
            Integer oldTeam = playerTeams.get(uuid);
            if (oldTeam != null) {
                Set<UUID> oldTeamSet = teams.get(oldTeam);
                if (oldTeamSet != null) {
                    oldTeamSet.remove(uuid);
                    if (oldTeamSet.isEmpty()) {
                        teams.remove(oldTeam);
                    }
                }
            }

            // Add to new team - use thread-safe set
            Set<UUID> newTeamSet = teams.computeIfAbsent(newTeam, k -> ConcurrentHashMap.newKeySet());
            newTeamSet.add(uuid);
            playerTeams.put(uuid, newTeam);

            debug("Moved player from Team " + oldTeam + " to Team " + newTeam);
        }
    }

    /**
     * Check if teams need balancing
     * @param alivePlayers Set of alive player UUIDs
     * @return true if teams are unbalanced (difference of 2+ players)
     */
    public boolean needsBalancing(Set<UUID> alivePlayers) {
        if (!isTeamMode() || teams.size() < 2) return false;

        int maxSize = 0, minSize = Integer.MAX_VALUE;

        for (int teamNum : teams.keySet()) {
            int aliveCount = 0;
            for (UUID member : teams.get(teamNum)) {
                if (alivePlayers.contains(member)) {
                    aliveCount++;
                }
            }
            if (aliveCount > 0) { // Only count teams with alive players
                maxSize = Math.max(maxSize, aliveCount);
                minSize = Math.min(minSize, aliveCount);
            }
        }

        return (maxSize - minSize) >= 2;
    }
}