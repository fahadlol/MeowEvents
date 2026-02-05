package me.oblueberrey.meowMcEvents.managers;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.utils.ConfigManager;
import me.oblueberrey.meowMcEvents.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the live sidebar scoreboard during events
 * Styled like the practice scoreboard with clean formatting
 */
public class ScoreboardManager {

    private final MeowMCEvents plugin;
    // Thread-safe map to prevent ConcurrentModificationException during updates
    private final Map<UUID, Scoreboard> playerScoreboards = new ConcurrentHashMap<>();
    private BukkitTask updateTask;
    private volatile long eventStartTime;
    private volatile boolean active = false;

    // Unicode symbols matching the practice scoreboard
    private static final String SKULL = "\u2620";      // ☠
    private static final String SWORDS = "\u2694";     // ⚔
    private static final String DIAMOND = "\u25C6";    // ◆
    private static final String STAR = "\u2605";       // ★
    private static final String INFO = "\u24D8";       // ⓘ

    public ScoreboardManager(MeowMCEvents plugin) {
        this.plugin = plugin;
    }

    /**
     * Start the scoreboard for all event participants
     */
    public void startScoreboard(Set<UUID> players) {
        if (active) return;

        active = true;
        eventStartTime = System.currentTimeMillis();

        // Create scoreboards for all players
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                createScoreboard(player);
            }
        }

        // Start update task (every 20 ticks = 1 second)
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllScoreboards, 0L, 20L);

        debug("Scoreboard started for " + players.size() + " players");
    }

    /**
     * Add a player to the scoreboard (for spectators joining mid-event)
     */
    public void addPlayer(Player player) {
        if (!active) return;
        createScoreboard(player);
    }

    /**
     * Remove a player's scoreboard
     */
    public void removePlayer(Player player) {
        if (player == null) return;

        playerScoreboards.remove(player.getUniqueId());

        // Reset to default scoreboard
        org.bukkit.scoreboard.ScoreboardManager defaultManager = Bukkit.getScoreboardManager();
        if (defaultManager != null) {
            player.setScoreboard(defaultManager.getNewScoreboard());
        }
    }

    /**
     * Stop all scoreboards
     */
    public void stopScoreboard() {
        if (!active) return;

        active = false;

        // Cancel update task
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        // Reset all player scoreboards
        org.bukkit.scoreboard.ScoreboardManager defaultManager = Bukkit.getScoreboardManager();
        if (defaultManager != null) {
            for (UUID uuid : new HashSet<>(playerScoreboards.keySet())) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.setScoreboard(defaultManager.getNewScoreboard());
                }
            }
        }

        playerScoreboards.clear();
        debug("Scoreboard stopped");
    }

    /**
     * Create a scoreboard for a player
     */
    private void createScoreboard(Player player) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard scoreboard = manager.getNewScoreboard();

        // Create objective with styled title matching practice scoreboard
        String title = MessageUtils.colorize("&6&lEVENT");
        Objective objective = scoreboard.registerNewObjective("meowevents", "dummy", title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        playerScoreboards.put(player.getUniqueId(), scoreboard);
        player.setScoreboard(scoreboard);
    }

    /**
     * Update all scoreboards
     * Styled like the practice scoreboard with clean formatting
     */
    private void updateAllScoreboards() {
        if (!active) return;

        EventManager eventManager = plugin.getEventManager();
        TeamManager teamManager = plugin.getTeamManager();
        EventStatsManager statsManager = plugin.getEventStatsManager();

        if (eventManager == null || !eventManager.isEventRunning()) {
            stopScoreboard();
            return;
        }

        int alivePlayers = eventManager.getAlivePlayerCount();
        int totalPlayers = eventManager.getJoinedPlayerCount();
        int teamSize = eventManager.getTeamSize();
        String elapsedTime = getElapsedTime();

        for (Map.Entry<UUID, Scoreboard> entry : playerScoreboards.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;

            Scoreboard scoreboard = entry.getValue();
            Objective objective = scoreboard.getObjective("meowevents");
            if (objective == null) continue;

            // Update teams for name colors
            updateNameColors(scoreboard, eventManager, teamManager);

            // Clear old scores
            for (String s : scoreboard.getEntries()) {
                // Don't clear entries that belong to teams (name colors)
                boolean isTeamEntry = false;
                for (Team team : scoreboard.getTeams()) {
                    if (team.hasEntry(s)) {
                        isTeamEntry = true;
                        break;
                    }
                }
                if (!isTeamEntry) {
                    scoreboard.resetScores(s);
                }
            }

            int score = 10;

            // Player's kills with skull icon
            int kills = statsManager != null ? statsManager.getKills(player.getUniqueId()) : 0;
            objective.getScore(ConfigManager.colorize("&c" + SKULL + " &fKills: &e" + kills)).setScore(score--);

            // Kill streak with swords icon
            int streak = plugin.getKillStreakManager().getStreak(player);
            objective.getScore(ConfigManager.colorize("&c" + SWORDS + " &fStreak: &e" + streak)).setScore(score--);

            // Blank line
            objective.getScore("§1 ").setScore(score--);

            // Players alive with diamond icon
            objective.getScore(ConfigManager.colorize("&b" + DIAMOND + " &fAlive: &a" + alivePlayers)).setScore(score--);

            // Time elapsed with star icon
            objective.getScore(ConfigManager.colorize("&e" + STAR + " &fTime: &7" + elapsedTime)).setScore(score--);

            // Team info (if team mode)
            if (teamSize > 1) {
                int teamNum = teamManager.getTeam(player);
                if (teamNum != -1) {
                    ChatColor teamColor = teamManager.getTeamColor(teamNum);
                    int teamAlive = teamManager.getAliveTeamMemberCount(teamNum);
                    objective.getScore(ConfigManager.colorize("&d" + STAR + " &fTeam: " + teamColor + teamNum + " &7(" + teamAlive + " alive)")).setScore(score--);
                }
            }

            // Spectator indicator
            if (eventManager.isSpectator(player)) {
                objective.getScore(ConfigManager.colorize("&7[Spectating]")).setScore(score--);
            }

            // Blank line before footer
            objective.getScore("§2  ").setScore(score--);

            // Server/plugin branding with info icon
            String serverIP = plugin.getConfig().getString("server-ip", "meowmc.net");
            objective.getScore(ConfigManager.colorize("&7" + INFO + " &7" + serverIP)).setScore(score--);
        }
    }

    /**
     * Update player name colors on the scoreboard
     */
    private void updateNameColors(Scoreboard scoreboard, EventManager eventManager, TeamManager teamManager) {
        // Create/get teams for colors
        Team yellowTeam = getOrCreateTeam(scoreboard, "yellow", ChatColor.YELLOW);
        Team greyTeam = getOrCreateTeam(scoreboard, "grey", ChatColor.GRAY);

        // Team mode teams
        Map<Integer, Team> teamModeTeams = new HashMap<>();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            String entry = onlinePlayer.getName();
            
            // First, remove from all teams to avoid conflicts
            for (Team team : scoreboard.getTeams()) {
                if (team.hasEntry(entry)) {
                    team.removeEntry(entry);
                }
            }

            if (eventManager.isSpectator(onlinePlayer)) {
                greyTeam.addEntry(entry);
            } else if (eventManager.isPlayerInEvent(onlinePlayer)) {
                if (teamManager.isTeamMode()) {
                    int teamNum = teamManager.getTeam(onlinePlayer);
                    if (teamNum != -1) {
                        Team t = teamModeTeams.computeIfAbsent(teamNum, n -> 
                            getOrCreateTeam(scoreboard, "team_" + n, teamManager.getTeamColor(n)));
                        t.addEntry(entry);
                    } else {
                        yellowTeam.addEntry(entry);
                    }
                } else {
                    yellowTeam.addEntry(entry);
                }
            }
        }
    }

    private Team getOrCreateTeam(Scoreboard scoreboard, String name, ChatColor color) {
        Team team = scoreboard.getTeam(name);
        if (team == null) {
            team = scoreboard.registerNewTeam(name);
        }
        team.setColor(color);
        return team;
    }

    /**
     * Get elapsed time as formatted string
     */
    private String getElapsedTime() {
        long elapsed = System.currentTimeMillis() - eventStartTime;
        long seconds = (elapsed / 1000) % 60;
        long minutes = (elapsed / 1000) / 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Debug logging
     */
    private void debug(String message) {
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG:SCOREBOARD] " + message);
        }
    }

    /**
     * Check if scoreboard is active
     */
    public boolean isActive() {
        return active;
    }
}
