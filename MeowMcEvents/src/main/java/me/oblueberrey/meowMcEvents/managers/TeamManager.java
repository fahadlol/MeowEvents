package me.oblueberrey.meowMcEvents.managers;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

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
}