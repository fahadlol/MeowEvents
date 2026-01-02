package me.oblueberrey.meowMcEvents.managers;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class EventManager {

    private final MeowMCEvents plugin;
    private final TeamManager teamManager;
    private final RankManager rankManager;
    private final BorderManager borderManager;
    private final KitManager kitManager;

    private boolean eventRunning;
    private boolean buildingAllowed;
    private boolean breakingAllowed;
    private int teamSize;
    private Set<UUID> alivePlayers;
    private BukkitTask winnerCheckTask;

    public EventManager(MeowMCEvents plugin, TeamManager teamManager,
                        RankManager rankManager, BorderManager borderManager, KitManager kitManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.rankManager = rankManager;
        this.borderManager = borderManager;
        this.kitManager = kitManager;
        this.eventRunning = false;
        this.buildingAllowed = plugin.getConfigManager().isDefaultBuildingAllowed();
        this.breakingAllowed = plugin.getConfigManager().isDefaultBreakingAllowed();
        this.teamSize = 1; // Default 1v1 (solo mode)
        this.alivePlayers = new HashSet<>();
    }

    /**
     * Start the event
     */
    public void startEvent() {
        if (eventRunning) {
            return;
        }

        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

        // Check minimum player requirement
        if (onlinePlayers.size() < 2) {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfigManager().getMessage("not-enough-players")));
            return;
        }

        // Initialize event state
        eventRunning = true;
        alivePlayers.clear();
        rankManager.clearAllRanks();

        // Assign teams if team size > 1
        if (teamSize > 1) {
            teamManager.assignTeams(onlinePlayers, teamSize);
        } else {
            teamManager.clearTeams();
        }

        // Teleport all players to spawn
        Location spawn = plugin.getConfigManager().getSpawnLocation();
        for (Player player : onlinePlayers) {
            player.teleport(spawn);
            alivePlayers.add(player.getUniqueId());

            // Give kit using XyrisKits API
            kitManager.giveKit(player);

            // Send team notification if in team mode
            if (teamSize > 1) {
                int team = teamManager.getTeam(player);
                ChatColor teamColor = teamManager.getTeamColor(team);
                player.sendMessage(teamColor + "You are on Team " + team);
            } else {
                player.sendMessage(ChatColor.GOLD + "Solo Mode - Last player standing wins!");
            }
        }

        // Start border shrinking
        borderManager.startBorderShrink(spawn.getWorld(), spawn);

        // Broadcast start message
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfigManager().getMessage("event-start")));

        // Play sound
        for (Player player : onlinePlayers) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        }

        // Start winner check task (runs every second)
        startWinnerCheckTask();
    }

    /**
     * Stop the event
     */
    public void stopEvent() {
        if (!eventRunning) {
            return;
        }

        eventRunning = false;

        // Stop winner check task
        if (winnerCheckTask != null) {
            winnerCheckTask.cancel();
            winnerCheckTask = null;
        }

        // Reset border
        World world = plugin.getServer().getWorlds().get(0);
        borderManager.resetBorder(world);

        // Clear teams and ranks
        teamManager.clearTeams();
        rankManager.clearAllRanks();
        alivePlayers.clear();

        // Broadcast stop message
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfigManager().getMessage("event-stop")));
    }

    /**
     * Start the repeating winner check task
     */
    private void startWinnerCheckTask() {
        winnerCheckTask = Bukkit.getScheduler().runTaskTimer(plugin,
                this::checkForWinner, 20L, 20L); // Run every 1 second
    }

    /**
     * Check if there's a winner
     */
    public void checkForWinner() {
        if (!eventRunning) {
            return;
        }

        // Team mode: check if only one team has alive players
        if (teamManager.isTeamMode()) {
            int aliveTeams = teamManager.getAliveTeamCount(alivePlayers);

            if (aliveTeams <= 1) {
                int winningTeam = teamManager.getWinningTeam(alivePlayers);
                if (winningTeam != -1) {
                    announceTeamWinner(winningTeam);
                } else {
                    // No teams left, stop event
                    stopEvent();
                }
            }
        }
        // Solo mode: check if only one player is alive
        else {
            if (alivePlayers.size() <= 1) {
                if (alivePlayers.size() == 1) {
                    UUID winnerUUID = alivePlayers.iterator().next();
                    Player winner = Bukkit.getPlayer(winnerUUID);
                    if (winner != null) {
                        announceSoloWinner(winner);
                    } else {
                        stopEvent();
                    }
                } else {
                    // No winner (everyone died somehow)
                    stopEvent();
                }
            }
        }
    }

    /**
     * Announce solo winner
     */
    private void announceSoloWinner(Player winner) {
        // Stop event FIRST to prevent multiple calls
        eventRunning = false;

        // Stop winner check task
        if (winnerCheckTask != null) {
            winnerCheckTask.cancel();
            winnerCheckTask = null;
        }

        String message = plugin.getConfigManager().getMessage("winner-solo")
                .replace("%player%", winner.getName());

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));

        // Send title to all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(
                    ChatColor.GOLD + "" + ChatColor.BOLD + "WINNER",
                    ChatColor.YELLOW + winner.getName(),
                    10, 70, 20
            );
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        // Cleanup after 5 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            World world = plugin.getServer().getWorlds().get(0);
            borderManager.resetBorder(world);
            teamManager.clearTeams();
            rankManager.clearAllRanks();
            alivePlayers.clear();

            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfigManager().getMessage("event-stop")));
        }, 100L);
    }

    /**
     * Announce team winner
     */
    private void announceTeamWinner(int teamNumber) {
        // Stop event FIRST to prevent multiple calls
        eventRunning = false;

        // Stop winner check task
        if (winnerCheckTask != null) {
            winnerCheckTask.cancel();
            winnerCheckTask = null;
        }

        ChatColor teamColor = teamManager.getTeamColor(teamNumber);
        String message = plugin.getConfigManager().getMessage("winner-team")
                .replace("%team%", teamColor + "Team " + teamNumber);

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));

        // Send title to all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(
                    ChatColor.GOLD + "" + ChatColor.BOLD + "WINNER",
                    teamColor + "Team " + teamNumber,
                    10, 70, 20
            );
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        // Cleanup after 5 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            World world = plugin.getServer().getWorlds().get(0);
            borderManager.resetBorder(world);
            teamManager.clearTeams();
            rankManager.clearAllRanks();
            alivePlayers.clear();

            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfigManager().getMessage("event-stop")));
        }, 100L);
    }

    /**
     * Kill a player (from GUI)
     */
    public void killPlayer(Player target, Player admin) {
        if (!eventRunning) {
            admin.sendMessage(ChatColor.RED + "No event is running!");
            return;
        }

        // Deal massive damage to kill player instantly
        target.damage(9999.0);

        admin.sendMessage(ChatColor.GREEN + "Killed " + target.getName());
    }

    /**
     * Mark player as dead
     */
    public void markPlayerDead(Player player) {
        alivePlayers.remove(player.getUniqueId());
    }

    /**
     * Broadcast kill message
     */
    public void broadcastKill(Player killer, Player victim, int killerRank) {
        String message = plugin.getConfigManager().getMessage("kill-broadcast")
                .replace("%killer%", killer.getName())
                .replace("%rank%", String.valueOf(killerRank))
                .replace("%victim%", victim.getName());

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    // Getters and setters

    public boolean isEventRunning() {
        return eventRunning;
    }

    public boolean isBuildingAllowed() {
        return buildingAllowed;
    }

    public void toggleBuilding() {
        this.buildingAllowed = !this.buildingAllowed;
    }

    public boolean isBreakingAllowed() {
        return breakingAllowed;
    }

    public void toggleBreaking() {
        this.breakingAllowed = !this.breakingAllowed;
    }

    public int getTeamSize() {
        return teamSize;
    }

    public void setTeamSize(int teamSize) {
        this.teamSize = teamSize;
    }
}