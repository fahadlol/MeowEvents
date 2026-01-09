package me.oblueberrey.meowMcEvents.managers;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class TeamManager {

    private final Map<Integer, Set<UUID>> teams; // Team number -> Set of player UUIDs
    private final Map<UUID, Integer> playerTeams; // Player UUID -> Team number

    public TeamManager() {
        this.teams = new HashMap<>();
        this.playerTeams = new HashMap<>();
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
        clearTeams();

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

            // Get or create team set
            Set<UUID> team = teams.computeIfAbsent(teamNumber, k -> new HashSet<>());

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
     * Cycles through 8 colors
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
                ChatColor.WHITE
        };

        if (teamNumber >= 1 && teamNumber <= colors.length) {
            return colors[teamNumber - 1];
        }

        return ChatColor.GRAY;
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
        if (!isTeamMode() || teams.size() < 2) {
            debug("Auto-balance skipped: not in team mode or less than 2 teams");
            return false;
        }

        // Calculate alive players per team
        Map<Integer, List<UUID>> alivePerTeam = new HashMap<>();
        for (int teamNum : teams.keySet()) {
            alivePerTeam.put(teamNum, new ArrayList<>());
            for (UUID member : teams.get(teamNum)) {
                if (alivePlayers.contains(member)) {
                    alivePerTeam.get(teamNum).add(member);
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
            movePlayerToTeam(playerToMove, minTeam);

            // Update local tracking
            largeTeamPlayers.remove(playerToMove);
            alivePerTeam.get(minTeam).add(playerToMove);

            // Notify the moved player
            Player player = org.bukkit.Bukkit.getPlayer(playerToMove);
            if (player != null && player.isOnline()) {
                ChatColor newColor = getTeamColor(minTeam);
                player.sendMessage(ChatColor.YELLOW + "[Auto-Balance] " + ChatColor.WHITE +
                        "You have been moved to " + newColor + "Team " + minTeam);
                player.sendTitle(
                        ChatColor.YELLOW + "Team Changed!",
                        newColor + "You are now on Team " + minTeam,
                        10, 40, 10
                );
            }

            debug("Auto-balanced: moved player to Team " + minTeam + " (was " + maxSize + " vs " + minSize + ")");
            balanced = true;
        }

        if (balanced) {
            org.bukkit.Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6&l[MeowEvent] &eTeams have been auto-balanced!"));
        }

        return balanced;
    }

    /**
     * Move a player to a different team
     * @param uuid Player UUID
     * @param newTeam New team number
     */
    public void movePlayerToTeam(UUID uuid, int newTeam) {
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

        // Add to new team
        Set<UUID> newTeamSet = teams.computeIfAbsent(newTeam, k -> new HashSet<>());
        newTeamSet.add(uuid);
        playerTeams.put(uuid, newTeam);

        debug("Moved player from Team " + oldTeam + " to Team " + newTeam);
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