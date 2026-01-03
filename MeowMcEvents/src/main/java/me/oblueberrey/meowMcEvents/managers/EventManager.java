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
    private Set<UUID> joinedPlayers; // Players who joined with /event
    private Set<UUID> alivePlayers; // Players currently alive in event
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
        this.teamSize = plugin.getConfigManager().getDefaultMode(); // Load from config
        this.joinedPlayers = new HashSet<>();
        this.alivePlayers = new HashSet<>();

        if (plugin.getConfigManager().shouldLogEvents()) {
            plugin.getLogger().info("[DEBUG:EVENT] EventManager initialized. Default mode: " + teamSize + ", Building: " + buildingAllowed + ", Breaking: " + breakingAllowed);
        }
    }

    /**
     * Add player to event queue
     */
    public void addPlayer(Player player) {
        joinedPlayers.add(player.getUniqueId());
        if (plugin.getConfigManager().shouldLogPlayers()) {
            plugin.getLogger().info("[DEBUG:PLAYER] " + player.getName() + " joined event queue. Total queued: " + joinedPlayers.size());
        }
    }

    /**
     * Remove player from event
     */
    public void removePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        joinedPlayers.remove(uuid);
        alivePlayers.remove(uuid);
        teamManager.removeFromTeam(player);
        rankManager.resetRank(player);
        if (plugin.getConfigManager().shouldLogPlayers()) {
            plugin.getLogger().info("[DEBUG:PLAYER] " + player.getName() + " removed from event. Alive: " + alivePlayers.size() + ", Queued: " + joinedPlayers.size());
        }
    }

    /**
     * Check if player has joined the event
     */
    public boolean hasPlayerJoined(Player player) {
        return joinedPlayers.contains(player.getUniqueId());
    }

    /**
     * Check if player is currently in the event
     */
    public boolean isPlayerInEvent(Player player) {
        return alivePlayers.contains(player.getUniqueId());
    }

    /**
     * Start the event
     */
    public void startEvent() {
        if (eventRunning) {
            if (plugin.getConfigManager().shouldLogEvents()) {
                plugin.getLogger().info("[DEBUG:EVENT] Attempted to start event but already running");
            }
            return;
        }

        List<Player> players = new ArrayList<>();
        for (UUID uuid : joinedPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }

        if (plugin.getConfigManager().shouldLogEvents()) {
            plugin.getLogger().info("[DEBUG:EVENT] Starting event with " + players.size() + " players");
        }

        // Check minimum player requirement
        if (players.size() < plugin.getConfigManager().getMinPlayers()) {
            if (plugin.getConfigManager().shouldLogEvents()) {
                plugin.getLogger().info("[DEBUG:EVENT] Not enough players. Required: " + plugin.getConfigManager().getMinPlayers() + ", Got: " + players.size());
            }
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfigManager().getMessage("not-enough-players")));
            return;
        }

        // Initialize event state
        eventRunning = true;
        alivePlayers.clear();
        rankManager.clearAllRanks();

        if (plugin.getConfigManager().shouldLogEvents()) {
            plugin.getLogger().info("[DEBUG:EVENT] Event state initialized. Team size: " + teamSize);
        }

        // Assign teams if team size > 1
        if (teamSize > 1) {
            teamManager.assignTeams(players, teamSize);
            if (plugin.getConfigManager().shouldLogTeams()) {
                plugin.getLogger().info("[DEBUG:TEAM] Teams assigned. Total teams: " + teamManager.getTeamCount());
            }
        } else {
            teamManager.clearTeams();
            if (plugin.getConfigManager().shouldLogTeams()) {
                plugin.getLogger().info("[DEBUG:TEAM] Solo mode - no teams");
            }
        }

        // Teleport all joined players to spawn and give kits
        Location spawn = plugin.getConfigManager().getSpawnLocation();
        if (spawn == null || spawn.getWorld() == null) {
            plugin.getLogger().severe("[ERROR] Spawn location or world is null! Check config.yml spawn settings.");
            eventRunning = false;
            return;
        }

        if (plugin.getConfigManager().shouldLogEvents()) {
            plugin.getLogger().info("[DEBUG:EVENT] Teleporting players to spawn: " + spawn.getWorld().getName() + " at " + spawn.getBlockX() + ", " + spawn.getBlockY() + ", " + spawn.getBlockZ());
        }

        for (Player player : players) {
            player.teleport(spawn);
            alivePlayers.add(player.getUniqueId());

            // Give kit via command
            kitManager.giveSelectedKit(player);

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
        for (Player player : players) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        }

        // Start winner check task (runs every second)
        startWinnerCheckTask();

        if (plugin.getConfigManager().shouldLogEvents()) {
            plugin.getLogger().info("[DEBUG:EVENT] Event started successfully. Alive players: " + alivePlayers.size());
        }
    }

    /**
     * Stop the event
     */
    public void stopEvent() {
        if (!eventRunning) {
            if (plugin.getConfigManager().shouldLogEvents()) {
                plugin.getLogger().info("[DEBUG:EVENT] Attempted to stop event but no event running");
            }
            return;
        }

        if (plugin.getConfigManager().shouldLogEvents()) {
            plugin.getLogger().info("[DEBUG:EVENT] Stopping event. Alive players before cleanup: " + alivePlayers.size());
        }

        eventRunning = false;

        // Stop winner check task
        if (winnerCheckTask != null) {
            winnerCheckTask.cancel();
            winnerCheckTask = null;
        }

        // Reset border - use spawn location world instead of first world
        Location spawn = plugin.getConfigManager().getSpawnLocation();
        World world = spawn != null && spawn.getWorld() != null ? spawn.getWorld() : plugin.getServer().getWorlds().get(0);
        borderManager.resetBorder(world);

        // Teleport all alive players back to player spawn
        Location playerSpawn = plugin.getConfigManager().getPlayerSpawnLocation();
        if (playerSpawn == null || playerSpawn.getWorld() == null) {
            plugin.getLogger().warning("[WARNING] Player spawn location is null! Players may not be teleported correctly.");
        }

        for (UUID uuid : alivePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                if (playerSpawn != null && playerSpawn.getWorld() != null) {
                    player.teleport(playerSpawn);
                }
                player.getInventory().clear();
                player.setHealth(20.0);
                player.setFoodLevel(20);
            }
        }

        // Clear teams, ranks, and players
        teamManager.clearTeams();
        rankManager.clearAllRanks();
        joinedPlayers.clear();
        alivePlayers.clear();

        // Broadcast stop message
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfigManager().getMessage("event-stop")));

        if (plugin.getConfigManager().shouldLogEvents()) {
            plugin.getLogger().info("[DEBUG:EVENT] Event stopped and cleaned up successfully");
        }
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

            if (plugin.getConfigManager().shouldLogEvents()) {
                plugin.getLogger().info("[DEBUG:EVENT] Winner check - Team mode. Alive teams: " + aliveTeams + ", Alive players: " + alivePlayers.size());
            }

            if (aliveTeams <= 1) {
                int winningTeam = teamManager.getWinningTeam(alivePlayers);
                if (winningTeam != -1) {
                    if (plugin.getConfigManager().shouldLogEvents()) {
                        plugin.getLogger().info("[DEBUG:EVENT] Team winner detected: Team " + winningTeam);
                    }
                    announceTeamWinner(winningTeam);
                } else {
                    // No teams left, stop event
                    if (plugin.getConfigManager().shouldLogEvents()) {
                        plugin.getLogger().info("[DEBUG:EVENT] No teams left, stopping event");
                    }
                    stopEvent();
                }
            }
        }
        // Solo mode: check if only one player is alive
        else {
            if (plugin.getConfigManager().shouldLogEvents()) {
                plugin.getLogger().info("[DEBUG:EVENT] Winner check - Solo mode. Alive players: " + alivePlayers.size());
            }

            if (alivePlayers.size() <= 1) {
                if (alivePlayers.size() == 1) {
                    UUID winnerUUID = alivePlayers.iterator().next();
                    Player winner = Bukkit.getPlayer(winnerUUID);
                    if (winner != null) {
                        if (plugin.getConfigManager().shouldLogEvents()) {
                            plugin.getLogger().info("[DEBUG:EVENT] Solo winner detected: " + winner.getName());
                        }
                        announceSoloWinner(winner);
                    } else {
                        if (plugin.getConfigManager().shouldLogEvents()) {
                            plugin.getLogger().info("[DEBUG:EVENT] Winner player is offline, stopping event");
                        }
                        stopEvent();
                    }
                } else {
                    // No winner (everyone died somehow)
                    if (plugin.getConfigManager().shouldLogEvents()) {
                        plugin.getLogger().info("[DEBUG:EVENT] No players left, stopping event");
                    }
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

            // Teleport all players back
            Location playerSpawn = plugin.getConfigManager().getPlayerSpawnLocation();
            for (UUID uuid : alivePlayers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.teleport(playerSpawn);
                    player.getInventory().clear();
                    player.setHealth(20.0);
                    player.setFoodLevel(20);
                }
            }

            teamManager.clearTeams();
            rankManager.clearAllRanks();
            joinedPlayers.clear();
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

            // Teleport all players back
            Location playerSpawn = plugin.getConfigManager().getPlayerSpawnLocation();
            for (UUID uuid : alivePlayers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.teleport(playerSpawn);
                    player.getInventory().clear();
                    player.setHealth(20.0);
                    player.setFoodLevel(20);
                }
            }

            teamManager.clearTeams();
            rankManager.clearAllRanks();
            joinedPlayers.clear();
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
        if (plugin.getConfigManager().shouldLogPlayers()) {
            plugin.getLogger().info("[DEBUG:PLAYER] " + player.getName() + " marked as dead. Remaining alive: " + alivePlayers.size());
        }
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

    public int getJoinedPlayerCount() {
        return joinedPlayers.size();
    }
}