package me.oblueberrey.meowMcEvents.managers;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.listeners.ArenaBoundaryListener;
import me.oblueberrey.meowMcEvents.listeners.SpectatorCompassListener;
import me.oblueberrey.meowMcEvents.utils.EventFeedback;
import me.oblueberrey.meowMcEvents.utils.EventState;
import me.oblueberrey.meowMcEvents.utils.LogManager;
import me.oblueberrey.meowMcEvents.utils.MessageUtils;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventManager {

    private final MeowMCEvents plugin;
    private final TeamManager teamManager;
    private final KillStreakManager killStreakManager;
    private final BorderManager borderManager;
    private final KitManager kitManager;
    private final EventStatsManager eventStatsManager;
    private final EventFeedback eventFeedback;

    private volatile EventState state;
    private volatile boolean buildingAllowed;
    private volatile boolean breakingAllowed;
    private volatile boolean naturalRegenAllowed;
    private volatile int teamSize;
    private final Set<UUID> joinedPlayers; // Players who joined with /event (thread-safe)
    private final Set<UUID> alivePlayers; // Players currently alive in event (thread-safe)
    private final Set<UUID> spectators; // Players spectating the event (thread-safe)
    private BukkitTask winnerCheckTask;
    private BukkitTask countdownTask;
    private volatile boolean gracePeriodActive;
    private BukkitTask gracePeriodTask;
    private final Set<UUID> pendingRespawn; // Players who died in event, awaiting respawn
    private final Set<UUID> fallDamageImmune; // Temporary fall damage immunity after teleport
    private final Set<UUID> spectatorGracePeriod; // Spectators with temporary invulnerability
    private ArenaManager arenaManager;
    private ArenaBoundaryListener arenaBoundaryListener;

    // Lock to prevent race conditions in winner detection
    private final Object winnerLock = new Object();
    private final AtomicBoolean winnerAnnounced = new AtomicBoolean(false);

    public EventManager(MeowMCEvents plugin, TeamManager teamManager,
                        KillStreakManager killStreakManager, BorderManager borderManager, KitManager kitManager,
                        EventStatsManager eventStatsManager, EventFeedback eventFeedback) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.killStreakManager = killStreakManager;
        this.borderManager = borderManager;
        this.kitManager = kitManager;
        this.eventStatsManager = eventStatsManager;
        this.eventFeedback = eventFeedback;
        this.state = EventState.IDLE;
        this.buildingAllowed = plugin.getConfigManager().isDefaultBuildingAllowed();
        this.breakingAllowed = plugin.getConfigManager().isDefaultBreakingAllowed();
        this.naturalRegenAllowed = plugin.getConfigManager().isDefaultNaturalRegenAllowed();
        this.teamSize = plugin.getConfigManager().getDefaultMode(); // Load from config
        // Use thread-safe sets to prevent ConcurrentModificationException
        this.joinedPlayers = ConcurrentHashMap.newKeySet();
        this.alivePlayers = ConcurrentHashMap.newKeySet();
        this.spectators = ConcurrentHashMap.newKeySet();
        this.pendingRespawn = ConcurrentHashMap.newKeySet();
        this.fallDamageImmune = ConcurrentHashMap.newKeySet();
        this.spectatorGracePeriod = ConcurrentHashMap.newKeySet();

        if (plugin.getConfigManager().shouldLogEvents()) {
            plugin.getLogger().info("[DEBUG:EVENT] EventManager initialized. Default mode: " + teamSize + ", Building: " + buildingAllowed + ", Breaking: " + breakingAllowed + ", NaturalRegen: " + naturalRegenAllowed);
        }
    }

    /**
     * Start the countdown for players to join
     */
    public void startCountdown() {
        if (!plugin.getLicenseManager().canStartEvent()) {
            return;
        }
        if (state != EventState.IDLE) {
            if (plugin.getConfigManager().shouldLogEvents()) {
                plugin.getLogger().info("[DEBUG:EVENT] Countdown/event already active (State: " + state + ")");
            }
            return;
        }

        state = EventState.COUNTDOWN;
        joinedPlayers.clear();

        int countdownSeconds = plugin.getConfigManager().getCountdownSeconds();

        // Broadcast event starting
        String title = MessageUtils.colorize("&6&lEVENT STARTING");
        eventFeedback.broadcastAnnouncement(
                "&#666666\u2699 " + title + " &#666666\u2699",
                "&#FF9944An event is about to begin!",
                "&#AAAAAAType &#FFE566/event &#AAAAAAto join!",
                "&#AAAAAAStarting in &#FF5555" + countdownSeconds + " seconds"
        );

        // Play sound and show title to all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            eventFeedback.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            eventFeedback.sendTitle(player, "&6&lEVENT STARTING", "&eUse /event to join!", 10, 40, 10);
        }

        if (plugin.getConfigManager().shouldLogEvents()) {
            plugin.getLogger().info("[DEBUG:EVENT] CountdownL started: " + countdownSeconds + " seconds");
        }

        // Cancel any existing countdown task (safety check)
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }

        // Start countdown task
        final int[] timeLeft = {countdownSeconds};
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            timeLeft[0]--;

            // Broadcast at specific intervals
            if (timeLeft[0] == 30 || timeLeft[0] == 15 || timeLeft[0] == 10 ||
                timeLeft[0] == 5 || timeLeft[0] == 4 || timeLeft[0] == 3 ||
                timeLeft[0] == 2 || timeLeft[0] == 1) {
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                        "&6&l[MeowEvent] &c" + timeLeft[0] + " seconds &eremaining to join! Use /event to join!"));

                // Play countdown tick sound and show title to joined players
                for (UUID uuid : joinedPlayers) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        eventFeedback.playCountdownTick(player, timeLeft[0]);
                        eventFeedback.sendCountdownTitle(player, timeLeft[0]);
                    }
                }
            }

            // Countdown finished
            if (timeLeft[0] <= 0) {
                countdownTask.cancel();
                countdownTask = null;

                // Start the actual event
                startEvent();
            }
        }, 20L, 20L); // Run every second
    }

    /**
     * Cancel the countdown (clears players)
     */
    public void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        state = EventState.IDLE;
        joinedPlayers.clear();

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                "&c&l[MeowEvent] &eEvent countdown has been cancelled!"));

        if (plugin.getConfigManager().shouldLogEvents()) {
            plugin.getLogger().info("[DEBUG:EVENT] Countdown cancelled");
        }
    }

    /**
     * Stop countdown task only (keeps players for forcestart)
     */
    public void stopCountdownOnly() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        // Don't clear joinedPlayers - forcestart needs them
        // Don't change state - startEvent will handle it

        if (plugin.getConfigManager().shouldLogEvents()) {
            plugin.getLogger().info("[DEBUG:EVENT] Countdown task stopped (forcestart)");
        }
    }

    /**
     * Check if countdown is active (players can join)
     */
    public boolean isCountdownActive() {
        return state == EventState.COUNTDOWN;
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
        fallDamageImmune.remove(uuid);
        teamManager.removeFromTeam(player);
        killStreakManager.resetStreak(player);

        // Clear potion effects
        player.getActivePotionEffects().forEach(effect ->
            player.removePotionEffect(effect.getType()));

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
     * Add player as spectator
     * Uses adventure mode with flying enabled, locked inventory
     * Compass in slot 4, red dye (leave) in slot 8
     * Spectators are hidden from non-spectators (only other spectators can see them)
     */
    public void addSpectator(Player player) {
        spectators.add(player.getUniqueId());

        // Set gamemode from config (ADVENTURE or SPECTATOR)
        String gamemodeConfig = plugin.getConfigManager().getSpectatorGamemode();
        if ("SPECTATOR".equalsIgnoreCase(gamemodeConfig)) {
            player.setGameMode(GameMode.SPECTATOR);
        } else {
            // Default to ADVENTURE mode with flying
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(true);
            player.setFlying(true);
            // Set flight speed from config
            float flightSpeed = plugin.getConfigManager().getSpectatorFlightSpeed();
            player.setFlySpeed(flightSpeed);
        }

        // Clear and give spectator items (only if not in SPECTATOR gamemode)
        player.getInventory().clear();
        if (!"SPECTATOR".equalsIgnoreCase(gamemodeConfig)) {
            int compassSlot = plugin.getConfigManager().getCompassSlot();
            int leaveSlot = plugin.getConfigManager().getLeaveSlot();
            player.getInventory().setItem(compassSlot, SpectatorCompassListener.createSpectatorCompass());
            player.getInventory().setItem(leaveSlot, SpectatorCompassListener.createLeaveDye());
        }

        // Hide spectator from all non-spectators (alive players and non-event players)
        // Only other spectators can see this spectator
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(player)) {
                if (spectators.contains(onlinePlayer.getUniqueId())) {
                    // Other spectators can see this spectator
                    onlinePlayer.showPlayer(plugin, player);
                    player.showPlayer(plugin, onlinePlayer);
                } else {
                    // Non-spectators cannot see this spectator
                    onlinePlayer.hidePlayer(plugin, player);
                }
            }
        }

        // Add to boss bar and play spectator feedback
        eventFeedback.addPlayerToBossBar(player);
        eventFeedback.onBecomeSpectator(player);

        // Add to scoreboard
        ScoreboardManager scoreboardManager = plugin.getScoreboardManager();
        if (scoreboardManager != null && scoreboardManager.isActive()) {
            scoreboardManager.addPlayer(player);
        }

        // Add spectator grace period (temporary invulnerability)
        spectatorGracePeriod.add(player.getUniqueId());
        int graceTicks = plugin.getConfigManager().getSpectatorGracePeriodTicks();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            spectatorGracePeriod.remove(player.getUniqueId());
        }, graceTicks);

        if (plugin.getConfigManager().shouldLogPlayers()) {
            plugin.getLogger().info("[DEBUG:PLAYER] " + player.getName() + " added as spectator (adventure+fly, hidden from non-spectators, grace: " + graceTicks + " ticks). Total spectators: " + spectators.size());
        }
    }

    /**
     * Check if spectator is in grace period (invulnerable)
     */
    public boolean isSpectatorInGracePeriod(Player player) {
        return spectatorGracePeriod.contains(player.getUniqueId());
    }

    /**
     * Remove player from spectators
     */
    public void removeSpectator(Player player) {
        spectators.remove(player.getUniqueId());
        spectatorGracePeriod.remove(player.getUniqueId());

        // Reset game mode and flight
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setFlySpeed(0.1f); // Reset to default flight speed
        player.getInventory().clear();

        // Clear potion effects
        player.getActivePotionEffects().forEach(effect ->
            player.removePotionEffect(effect.getType()));

        // Make visible to all players again
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.showPlayer(plugin, player);
        }

        // Remove from boss bar
        eventFeedback.removePlayerFromBossBar(player);

        // Remove from scoreboard
        ScoreboardManager scoreboardManager = plugin.getScoreboardManager();
        if (scoreboardManager != null) {
            scoreboardManager.removePlayer(player);
        }

        // Clear spectator compass tracking
        if (plugin.getSpectatorCompassListener() != null) {
            plugin.getSpectatorCompassListener().clearSpectatorIndex(player);
        }

        if (plugin.getConfigManager().shouldLogPlayers()) {
            plugin.getLogger().info("[DEBUG:PLAYER] " + player.getName() + " removed from spectators. Remaining spectators: " + spectators.size());
        }
    }

    /**
     * Check if player is a spectator
     */
    public boolean isSpectator(Player player) {
        return spectators.contains(player.getUniqueId());
    }

    /**
     * Get spectator count
     */
    public int getSpectatorCount() {
        return spectators.size();
    }

    /**
     * Start the event
     */
    public void startEvent() {
        LogManager log = plugin.getLogManager();

        if (state == EventState.RUNNING || state == EventState.ENDING) {
            if (log != null) log.warn(LogManager.Category.EVENTS, "startEvent() called but state is " + state + " - aborting");
            return;
        }

        List<Player> players = new ArrayList<>();
        for (UUID uuid : joinedPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                players.add(player);
            } else {
                if (log != null) log.warn(LogManager.Category.PLAYERS, "Player with UUID " + uuid + " is null or offline during event start");
            }
        }

        if (log != null) log.info(LogManager.Category.EVENTS, "Starting event with " + players.size() + " online players (joined: " + joinedPlayers.size() + ")");

        // Check minimum player requirement
        if (players.size() < plugin.getConfigManager().getMinPlayers()) {
            if (log != null) log.error(LogManager.Category.EVENTS, "NOT ENOUGH PLAYERS - Required: " + plugin.getConfigManager().getMinPlayers() + ", Got: " + players.size());
            Bukkit.broadcastMessage(MessageUtils.colorize(MessageUtils.PREFIX + plugin.getConfigManager().getMessage("not-enough-players")));
            state = EventState.IDLE;
            return;
        }

        // Initialize event state
        state = EventState.RUNNING;
        winnerAnnounced.set(false); // Reset winner flag for new event
        alivePlayers.clear();
        killStreakManager.clearAllStreaks();
        eventStatsManager.reset();

        // Reset kill feed state (first blood, revenge tracking)
        KillFeedManager killFeedManager = plugin.getKillFeedManager();
        if (killFeedManager != null) {
            killFeedManager.reset();
        }

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
            state = EventState.IDLE;
            return;
        }

        if (plugin.getConfigManager().shouldLogEvents()) {
            plugin.getLogger().info("[DEBUG:EVENT] Teleporting players to spawn: " + spawn.getWorld().getName() + " at " + spawn.getBlockX() + ", " + spawn.getBlockY() + ", " + spawn.getBlockZ());
        }

        for (Player player : players) {
            player.teleport(spawn);
            alivePlayers.add(player.getUniqueId());

            // Clear potion effects for a clean start
            player.getActivePotionEffects().forEach(effect ->
                player.removePotionEffect(effect.getType()));

            // Reset health and hunger
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20f);

            // Grant temporary fall damage immunity (3 seconds)
            fallDamageImmune.add(player.getUniqueId());

            // Send team notification if in team mode
            if (teamSize > 1) {
                int team = teamManager.getTeam(player);
                MessageUtils.sendInfo(player, "You are on &#FFE566Team " + team);
            } else {
                MessageUtils.sendInfo(player, "&#FFE566Solo Mode &#AAAAAA- Last one standing wins!");
            }
        }
        
        // Give kits after a short delay to ensure teleport is complete
        // This prevents kit command issues from player not being fully loaded at location
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : players) {
                if (player != null && player.isOnline() && isPlayerInEvent(player)) {
                    kitManager.giveSelectedKit(player);
                }
            }
        }, 5L); // 5 tick delay (0.25 seconds)

        // Register all participants for stats tracking
        eventStatsManager.registerParticipants(alivePlayers);

        // Start border shrinking
        borderManager.startBorderShrink(spawn.getWorld(), spawn);

        // Broadcast start message
        String startTitle = MessageUtils.colorize("&a&lEVENT BEGUN");
        eventFeedback.broadcastAnnouncement(
                "&#666666\u2694 " + startTitle + " &#666666\u2694",
                "&#FF9944The battle has commenced!",
                "&#AAAAAAGood luck to all participants",
                "&#AAAAAA" + (teamSize == 1 ? "Solo Mode" : teamSize + "v" + teamSize + " Teams")
        );

        // Play event start effects for all players
        for (Player player : players) {
            eventFeedback.playEventStart(player);
            eventFeedback.sendTitle(player, "&a&lGO!", "&7Fight to survive!", 0, 30, 10);
        }

        // Spawn event start particles at spawn location
        eventFeedback.spawnEventStartParticles(spawn);

        // Create and start boss bar with player tracking
        eventFeedback.createBossBar("&6&lMeowEvent &8| &aStarting...", BarColor.GREEN);
        for (Player player : players) {
            eventFeedback.addPlayerToBossBar(player);
        }
        eventFeedback.startBossBarUpdates(alivePlayers, spectators, players.size());

        // Start live scoreboard sidebar
        ScoreboardManager scoreboardManager = plugin.getScoreboardManager();
        if (scoreboardManager != null) {
            scoreboardManager.startScoreboard(alivePlayers);
        }

        // Start tab list formatting
        TabListManager tabListManager = plugin.getTabListManager();
        if (tabListManager != null) {
            tabListManager.startTabList(alivePlayers);
        }


        // Start winner check task (runs every second)
        startWinnerCheckTask();

        // Start grace period if configured
        startGracePeriod();

        // Remove fall damage immunity after 3 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            fallDamageImmune.clear();
            if (plugin.getConfigManager().shouldLogEvents()) {
                plugin.getLogger().info("[DEBUG:EVENT] Fall damage immunity expired for all players");
            }
        }, 60L); // 3 seconds

        // Start arena boundary enforcement if active arena is set
        if (arenaBoundaryListener != null && arenaManager != null
                && arenaManager.getActiveArena() != null
                && arenaManager.getActiveArena().isComplete()) {
            arenaBoundaryListener.startBoundaryCheck();
        }

        if (plugin.getConfigManager().shouldLogEvents()) {
            plugin.getLogger().info("[DEBUG:EVENT] Event started successfully. Alive players: " + alivePlayers.size());
        }
    }

    /**
     * Start the PvP grace period
     */
    private void startGracePeriod() {
        int gracePeriodSeconds = plugin.getConfigManager().getPvpGracePeriodSeconds();
        if (gracePeriodSeconds <= 0) {
            gracePeriodActive = false;
            return;
        }

        gracePeriodActive = true;

        // Broadcast grace period message
        String graceTitle = MessageUtils.colorize("&e&lGRACE PERIOD");
        eventFeedback.broadcastSmallAnnouncement(
                "&#666666\u2699 " + graceTitle + " &#666666\u2699",
                "&#FF9944Duration: &#FFE566" + gracePeriodSeconds + "s &#AAAAAA| &#FF9944PvP Disabled"
        );

        if (plugin.getConfigManager().shouldLogEvents()) {
            plugin.getLogger().info("[DEBUG:EVENT] Grace period started: " + gracePeriodSeconds + " seconds");
        }

        // Schedule end of grace period
        gracePeriodTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            gracePeriodActive = false;
            String pvpEnabledTitle = MessageUtils.colorize("&c&lPVP ENABLED");
            eventFeedback.broadcastSmallAnnouncement(
                    "&#666666\u2694 " + pvpEnabledTitle + " &#666666\u2694",
                    "&#FF9944The grace period has ended! Fight!"
            );

            if (plugin.getConfigManager().shouldLogEvents()) {
                plugin.getLogger().info("[DEBUG:EVENT] Grace period ended");
            }
        }, gracePeriodSeconds * 20L);
    }

    /**
     * Check if grace period is currently active
     */
    public boolean isGracePeriodActive() {
        return gracePeriodActive;
    }

    /**
     * Stop the event or countdown
     */
    public void stopEvent() {
        // If countdown is active, cancel it
        if (state == EventState.COUNTDOWN) {
            cancelCountdown();

            // Teleport waiting players back to player eventspectate
            Location playerSpawn = plugin.getConfigManager().getPlayerSpawnLocation();
            for (UUID uuid : joinedPlayers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline() && playerSpawn != null && playerSpawn.getWorld() != null) {
                    player.teleport(playerSpawn);
                }
            }
            joinedPlayers.clear();
            return;
        }

        if (state != EventState.RUNNING && state != EventState.ENDING) {
            if (plugin.getConfigManager().shouldLogEvents()) {
                plugin.getLogger().info("[DEBUG:EVENT] Attempted to stop event but no event running");
            }
            return;
        }

        if (plugin.getConfigManager().shouldLogEvents()) {
            plugin.getLogger().info("[DEBUG:EVENT] Stopping event. Alive players before cleanup: " + alivePlayers.size());
        }

        state = EventState.ENDING;

        // Stop winner check task
        if (winnerCheckTask != null) {
            winnerCheckTask.cancel();
            winnerCheckTask = null;
        }

        // Stop grace period
        if (gracePeriodTask != null) {
            gracePeriodTask.cancel();
            gracePeriodTask = null;
        }
        gracePeriodActive = false;

        // Stop arena boundary enforcement
        if (arenaBoundaryListener != null) {
            arenaBoundaryListener.stopBoundaryCheck();
        }
        pendingRespawn.clear();
        fallDamageImmune.clear();

        // Reset border - use spawn location world instead of first world
        Location spawn = plugin.getConfigManager().getSpawnLocation();
        World world = spawn != null && spawn.getWorld() != null ? spawn.getWorld() : null;
        if (world == null && !plugin.getServer().getWorlds().isEmpty()) {
            world = plugin.getServer().getWorlds().get(0);
        }
        if (world != null) {
            borderManager.resetBorder(world);
        }

        // Remove boss bar
        eventFeedback.removeBossBar();

        // Stop scoreboard
        ScoreboardManager scoreboardManager = plugin.getScoreboardManager();
        if (scoreboardManager != null) {
            scoreboardManager.stopScoreboard();
        }

        // Stop tab list formatting
        TabListManager tabListManager = plugin.getTabListManager();
        if (tabListManager != null) {
            tabListManager.stopTabList();
        }

        // Collect all players to send /spawn to (alive players + spectators)
        Set<Player> playersToSpawn = new HashSet<>();

        // Prepare alive players for /spawn
        for (UUID uuid : alivePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.getInventory().clear();
                player.setHealth(20.0);
                player.setFoodLevel(20);
                // Clear potion effects
                player.getActivePotionEffects().forEach(effect ->
                    player.removePotionEffect(effect.getType()));
                playersToSpawn.add(player);
            }
        }

        // Prepare spectators for /spawn - reset their state first
        for (UUID uuid : spectators) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Reset spectator mode
                player.setGameMode(GameMode.SURVIVAL);
                player.setAllowFlight(false);
                player.setFlying(false);
                player.getInventory().clear();
                // Clear potion effects
                player.getActivePotionEffects().forEach(effect ->
                    player.removePotionEffect(effect.getType()));
                // Make spectators visible to all players again
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.showPlayer(plugin, player);
                }
                // Remove from boss bar
                eventFeedback.removePlayerFromBossBar(player);
                // Clear spectator compass tracking
                if (plugin.getSpectatorCompassListener() != null) {
                    plugin.getSpectatorCompassListener().clearSpectatorIndex(player);
                }
                playersToSpawn.add(player);
            }
        }

        // Clear teams, ranks, players, and spectators BEFORE making them run /spawn
        teamManager.clearTeams();
        killStreakManager.clearAllStreaks();
        joinedPlayers.clear();
        alivePlayers.clear();
        spectators.clear();
        spectatorGracePeriod.clear();

        // Clear damage tracker
        DamageTracker damageTracker = plugin.getDamageTracker();
        if (damageTracker != null) {
            damageTracker.clearAll();
        }

        // Broadcast stop message
        Bukkit.broadcastMessage(MessageUtils.colorize(
                plugin.getConfigManager().getMessage("event-stop")));

        // Get configurable delay and command from config
        final int delayTicks = plugin.getConfigManager().getEndSpawnDelayTicks();
        final String endCommand = plugin.getConfigManager().getEndCommand();
        final Set<Player> finalPlayersToSpawn = playersToSpawn;
        final LogManager log = plugin.getLogManager();

        if (log != null) log.info(LogManager.Category.EVENTS, "Scheduling /" + endCommand + " for " + finalPlayersToSpawn.size() + " players in " + delayTicks + " ticks");

        // Skip command execution if end-command is empty
        if (endCommand == null || endCommand.isEmpty()) {
            if (log != null) log.info(LogManager.Category.EVENTS, "end-command is empty, skipping command execution");
            state = EventState.IDLE;
            return;
        }

        // Execute command 3 times with 1 tick delay between each
        // Use console dispatch to bypass other plugins blocking player commands
        for (int i = 0; i < 3; i++) {
            final int attempt = i + 1;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (log != null) log.info(LogManager.Category.EVENTS, "Executing /" + endCommand + " for players via console (attempt " + attempt + "/3)...");

                int successCount = 0;
                int failCount = 0;

                for (Player player : finalPlayersToSpawn) {
                    if (player != null && player.isOnline()) {
                        try {
                            // Use console dispatch with player name substitution
                            // Supports %player% placeholder or appends player name if not present
                            String command = endCommand;
                            if (command.contains("%player%")) {
                                command = command.replace("%player%", player.getName());
                            } else {
                                // If no placeholder, append player name (e.g., "spawn" -> "spawn PlayerName")
                                command = command + " " + player.getName();
                            }
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                            successCount++;
                            if (log != null && attempt == 1) log.debug(LogManager.Category.PLAYERS, "Console executed /" + command + " for " + player.getName());
                        } catch (Exception e) {
                            failCount++;
                            if (log != null && attempt == 1) log.error(LogManager.Category.ERRORS, "Exception executing /" + endCommand + " for " + player.getName() + ": " + e.getMessage(), e);
                        }
                    } else {
                        failCount++;
                        if (log != null && attempt == 1) log.warn(LogManager.Category.PLAYERS, "Player in spawn list is null or offline - skipping");
                    }
                }

                if (log != null) log.info(LogManager.Category.EVENTS, "stopEvent() attempt " + attempt + " complete - /" + endCommand + " executed: " + successCount + " success, " + failCount + " failed");
            }, delayTicks + i); // Each attempt 1 tick apart
        }

        state = EventState.IDLE;
    }

    /**
     * Start the repeating winner check task
     */
    private void startWinnerCheckTask() {
        // Cancel any existing winner check task (safety check)
        if (winnerCheckTask != null) {
            winnerCheckTask.cancel();
            winnerCheckTask = null;
        }

        winnerCheckTask = Bukkit.getScheduler().runTaskTimer(plugin,
                this::checkForWinner, 20L, 20L); // Run every 1 second
    }

    /**
     * Check if there's a winner
     */
    public void checkForWinner() {
        if (state != EventState.RUNNING) {
            return;
        }

        // Synchronize winner detection to prevent race conditions
        synchronized (winnerLock) {
            // Double-check after acquiring lock
            if (state != EventState.RUNNING || winnerAnnounced.get()) {
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
                        // Prevent double announcement
                        if (winnerAnnounced.compareAndSet(false, true)) {
                            if (plugin.getConfigManager().shouldLogEvents()) {
                                plugin.getLogger().info("[DEBUG:EVENT] Team winner detected: Team " + winningTeam);
                            }
                            announceTeamWinner(winningTeam);
                        }
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
                            // Prevent double announcement
                            if (winnerAnnounced.compareAndSet(false, true)) {
                                if (plugin.getConfigManager().shouldLogEvents()) {
                                    plugin.getLogger().info("[DEBUG:EVENT] Solo winner detected: " + winner.getName());
                                }
                                announceSoloWinner(winner);
                            }
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
    }

    /**
     * Announce solo winner
     */
    private void announceSoloWinner(Player winner) {
        // Stop event FIRST to prevent multiple calls
        state = EventState.ENDING;

        // Stop winner check task
        if (winnerCheckTask != null) {
            winnerCheckTask.cancel();
            winnerCheckTask = null;
        }

        String message = plugin.getConfigManager().getMessage("winner-solo")
                .replace("%player%", winner.getName());

        // Broadcast solo winner
        String winTitle = MessageUtils.colorize("&6&lVICTORY");
        eventFeedback.broadcastAnnouncement(
                "&#666666\u2B50 " + winTitle + " &#666666\u2B50",
                "&#FFE566" + winner.getName() + " &#FF9944has won the event!",
                "&#AAAAAAcongratulations on your triumph!"
        );

        // Full winner celebration effects
        eventFeedback.onWin(winner);

        // Send winner title to all other players
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.equals(winner)) {
                eventFeedback.sendTitle(player, "&6&lWINNER", "&e" + winner.getName(), 10, 70, 20);
                eventFeedback.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        }

        // Announce rankings after 3 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            eventStatsManager.announceRankings(winner.getUniqueId(), false, -1, teamManager);
        }, 60L);

        // Cleanup after 8 seconds (give time for rankings to show)
        Bukkit.getScheduler().runTaskLater(plugin, this::cleanupAfterWinner, 160L);
    }

    /**
     * Announce team winner
     */
    private void announceTeamWinner(int teamNumber) {
        // Stop event FIRST to prevent multiple calls
        state = EventState.ENDING;

        // Stop winner check task
        if (winnerCheckTask != null) {
            winnerCheckTask.cancel();
            winnerCheckTask = null;
        }

        ChatColor teamColor = teamManager.getTeamColor(teamNumber);
        String message = plugin.getConfigManager().getMessage("winner-team")
                .replace("%team%", teamColor + "Team " + teamNumber);

        // Broadcast team winner
        String teamWinTitle = MessageUtils.colorize("&6&lVICTORY");
        eventFeedback.broadcastAnnouncement(
                "&#666666\u2B50 " + teamWinTitle + " &#666666\u2B50",
                teamColor + "Team " + teamNumber + " &#FF9944has won the event!",
                "&#AAAAAAteamwork led to success!"
        );

        // Get winning team members for celebration
        Set<UUID> winningTeamMembers = teamManager.getTeamMembers(teamNumber);

        // Full team winner celebration effects
        eventFeedback.onTeamWin(winningTeamMembers, teamNumber, teamColor);

        // Send title to all non-winning players
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!winningTeamMembers.contains(player.getUniqueId())) {
                eventFeedback.sendTitle(player, "&6&lWINNER", teamColor + "Team " + teamNumber, 10, 70, 20);
                eventFeedback.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        }

        // Announce rankings after 3 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            eventStatsManager.announceRankings(null, true, teamNumber, teamManager);
        }, 60L);

        // Cleanup after 8 seconds (give time for rankings to show)
        Bukkit.getScheduler().runTaskLater(plugin, this::cleanupAfterWinner, 160L);
    }

    /**
     * Common cleanup logic after a winner is announced
     */
    private void cleanupAfterWinner() {
        // Stop arena boundary enforcement
        if (arenaBoundaryListener != null) {
            arenaBoundaryListener.stopBoundaryCheck();
        }
        pendingRespawn.clear();
        fallDamageImmune.clear();

        // Remove boss bar
        eventFeedback.removeBossBar();

        // Stop scoreboard
        ScoreboardManager scoreboardManager = plugin.getScoreboardManager();
        if (scoreboardManager != null) {
            scoreboardManager.stopScoreboard();
        }

        // Stop tab list formatting
        TabListManager tabListManager = plugin.getTabListManager();
        if (tabListManager != null) {
            tabListManager.stopTabList();
        }

        // SECURITY: Use spawn location world instead of blindly getting first world
        Location spawn = plugin.getConfigManager().getSpawnLocation();
        World world = spawn != null && spawn.getWorld() != null ? spawn.getWorld() : null;
        if (world == null && !plugin.getServer().getWorlds().isEmpty()) {
            world = plugin.getServer().getWorlds().get(0);
        }
        if (world != null) {
            borderManager.resetBorder(world);
        }

        // Collect all players to send /spawn to (alive players + spectators)
        Set<Player> playersToSpawn = new HashSet<>();

        // Prepare alive players for /spawn
        for (UUID uuid : alivePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.getInventory().clear();
                player.setHealth(20.0);
                player.setFoodLevel(20);
                // Clear potion effects
                player.getActivePotionEffects().forEach(effect ->
                    player.removePotionEffect(effect.getType()));
                playersToSpawn.add(player);
            }
        }

        // Prepare spectators for /spawn - reset their state first
        for (UUID uuid : spectators) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Reset spectator mode
                player.setGameMode(GameMode.SURVIVAL);
                player.setAllowFlight(false);
                player.setFlying(false);
                player.getInventory().clear();
                // Clear potion effects
                player.getActivePotionEffects().forEach(effect ->
                    player.removePotionEffect(effect.getType()));
                // Make spectators visible to all players again
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.showPlayer(plugin, player);
                }
                // Remove from boss bar
                eventFeedback.removePlayerFromBossBar(player);
                // Clear spectator compass tracking
                if (plugin.getSpectatorCompassListener() != null) {
                    plugin.getSpectatorCompassListener().clearSpectatorIndex(player);
                }
                playersToSpawn.add(player);
            }
        }

        // Clear teams, ranks, players, and spectators BEFORE making them run /spawn
        // This ensures CommandBlockListener won't block them (state is already ENDING)
        teamManager.clearTeams();
        killStreakManager.clearAllStreaks();
        joinedPlayers.clear();
        alivePlayers.clear();
        spectators.clear();
        spectatorGracePeriod.clear();

        // Clear damage tracker
        DamageTracker damageTracker = plugin.getDamageTracker();
        if (damageTracker != null) {
            damageTracker.clearAll();
        }

        // Reset state to IDLE so new events can start
        state = EventState.IDLE;

        // Get configurable delay and command from config
        final int delayTicks = plugin.getConfigManager().getEndSpawnDelayTicks();
        final String endCommand = plugin.getConfigManager().getEndCommand();
        final Set<Player> finalPlayersToSpawn = playersToSpawn;
        final LogManager log = plugin.getLogManager();

        if (log != null) log.info(LogManager.Category.EVENTS, "cleanupAfterWinner: Scheduling /" + endCommand + " for " + finalPlayersToSpawn.size() + " players in " + delayTicks + " ticks");

        // Skip command execution if end-command is empty
        if (endCommand == null || endCommand.isEmpty()) {
            if (log != null) log.info(LogManager.Category.EVENTS, "end-command is empty, skipping command execution");
            return;
        }

        // Execute command 3 times with 1 tick delay between each
        // Use console dispatch to bypass other plugins blocking player commands
        for (int i = 0; i < 3; i++) {
            final int attempt = i + 1;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (log != null) log.info(LogManager.Category.EVENTS, "cleanupAfterWinner: Executing /" + endCommand + " for players via console (attempt " + attempt + "/3)...");

                int successCount = 0;
                int failCount = 0;

                for (Player player : finalPlayersToSpawn) {
                    if (player != null && player.isOnline()) {
                        try {
                            // Use console dispatch with player name substitution
                            // Supports %player% placeholder or appends player name if not present
                            String command = endCommand;
                            if (command.contains("%player%")) {
                                command = command.replace("%player%", player.getName());
                            } else {
                                // If no placeholder, append player name (e.g., "spawn" -> "spawn PlayerName")
                                command = command + " " + player.getName();
                            }
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                            successCount++;
                            if (log != null && attempt == 1) log.debug(LogManager.Category.PLAYERS, "Console executed /" + command + " for " + player.getName());
                        } catch (Exception e) {
                            failCount++;
                            if (log != null && attempt == 1) log.error(LogManager.Category.ERRORS, "Exception executing /" + endCommand + " for " + player.getName() + ": " + e.getMessage(), e);
                        }
                    } else {
                        failCount++;
                        if (log != null && attempt == 1) log.warn(LogManager.Category.PLAYERS, "Player in spawn list is null or offline - skipping");
                    }
                }

                if (log != null) log.info(LogManager.Category.EVENTS, "cleanupAfterWinner attempt " + attempt + " complete - /" + endCommand + " executed: " + successCount + " success, " + failCount + " failed");
            }, delayTicks + i); // Each attempt 1 tick apart
        }
    }

    /**
     * Kill a player (from GUI) - eliminates without going through damage/death cycle
     */
    public void killPlayer(Player target, Player admin) {
        if (state != EventState.RUNNING) {
            admin.sendMessage(me.oblueberrey.meowMcEvents.utils.ConfigManager.colorize(
                    "&#AAAAAA&#FF5555no event running"));
            return;
        }

        if (!isPlayerInEvent(target)) {
            admin.sendMessage(me.oblueberrey.meowMcEvents.utils.ConfigManager.colorize(
                    "&#AAAAAA&#FF5555player is not in event"));
            return;
        }

        // Eliminate player directly without dealing damage
        eliminatePlayer(target, null);

        admin.sendMessage(me.oblueberrey.meowMcEvents.utils.ConfigManager.colorize(
                "&#AAAAAA&#55FF55killed &#FFE566" + target.getName()));
    }
    
    /**
     * Directly eliminate a player without going through damage/death cycle.
     * Used by admin kill and as fallback for edge cases.
     * 
     * @param victim The player to eliminate
     * @param killer The killer (can be null for admin kills)
     */
    public void eliminatePlayer(Player victim, Player killer) {
        if (state != EventState.RUNNING && state != EventState.ENDING) return;
        if (!isPlayerInEvent(victim)) return;
        if (isSpectator(victim)) return;
        
        LogManager log = plugin.getLogManager();
        if (log != null) log.info(LogManager.Category.PLAYERS, "Eliminating " + victim.getName() + " directly (killer: " + (killer != null ? killer.getName() : "admin/system") + ")");
        
        // Clear inventory immediately
        victim.getInventory().clear();
        victim.getInventory().setArmorContents(null);
        
        // Heal and reset player state
        victim.setHealth(victim.getMaxHealth());
        victim.setFoodLevel(20);
        victim.setSaturation(20f);
        victim.setFireTicks(0);
        victim.getActivePotionEffects().forEach(effect -> 
            victim.removePotionEffect(effect.getType()));

        // Handle killer stats
        if (killer != null && !killer.equals(victim) && isPlayerInEvent(killer)) {
            killStreakManager.incrementStreak(killer);
            int killerStreak = killStreakManager.getStreak(killer);

            if (eventStatsManager != null) {
                eventStatsManager.recordKill(killer.getUniqueId());
            }

            broadcastKill(killer, victim, killerStreak);

            if (eventFeedback != null) {
                eventFeedback.onKill(killer, victim, killerStreak);
            }
        } else {
            // Admin kill or environmental - just broadcast elimination
            KillFeedManager killFeedManager = plugin.getKillFeedManager();
            if (killFeedManager != null) {
                killFeedManager.broadcastEnvironmentalDeath(victim, null);
            }

            if (eventFeedback != null) {
                eventFeedback.playDeathSound(victim);
                eventFeedback.sendDeathTitle(victim, null);
                eventFeedback.spawnKillParticles(victim.getLocation());
            }
        }

        // Reset victim streak
        killStreakManager.resetStreak(victim);

        // Mark as dead
        markPlayerDead(victim);

        // Convert to spectator
        addSpectator(victim);

        // Teleport to event spawn
        Location eventSpawn = plugin.getConfigManager().getSpawnLocation();
        if (eventSpawn != null && eventSpawn.getWorld() != null) {
            victim.teleport(eventSpawn);
        }

        victim.sendMessage(me.oblueberrey.meowMcEvents.utils.ConfigManager.colorize(
                "&#FF9944You were eliminated! &#AAAAAAYou are now spectating."));

        // Ensure spectator state persists for 3 seconds (protection against other plugins)
        for (int i = 1; i <= 6; i++) {
            final int checkNum = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!isEventRunning() || !victim.isOnline()) {
                    return;
                }
                if (isSpectator(victim)) {
                    ensureSpectatorState(victim, checkNum);
                }
            }, 10L * i); // 10, 20, 30, 40, 50, 60 ticks
        }

        // Check for winner
        checkForWinner();

        // Trigger auto-balance
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isEventRunning()) {
                triggerAutoBalance();
            }
        }, 20L);
    }
    
    /**
     * Ensure spectator has correct state and items (protection against other plugins resetting state)
     */
    private void ensureSpectatorState(Player player, int checkNum) {
        String gamemodeConfig = plugin.getConfigManager().getSpectatorGamemode();

        if ("SPECTATOR".equalsIgnoreCase(gamemodeConfig)) {
            if (player.getGameMode() != GameMode.SPECTATOR) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        } else {
            if (player.getGameMode() != GameMode.ADVENTURE) {
                player.setGameMode(GameMode.ADVENTURE);
            }
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
            }
            if (!player.isFlying()) {
                player.setFlying(true);
            }

            // Ensure spectator items
            int compassSlot = plugin.getConfigManager().getCompassSlot();
            int leaveSlot = plugin.getConfigManager().getLeaveSlot();

            if (player.getInventory().getItem(compassSlot) == null) {
                player.getInventory().setItem(compassSlot, SpectatorCompassListener.createSpectatorCompass());
            }
            if (player.getInventory().getItem(leaveSlot) == null) {
                player.getInventory().setItem(leaveSlot, SpectatorCompassListener.createLeaveDye());
            }
        }

        // Re-hide from non-spectators
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(player)) {
                if (isSpectator(onlinePlayer)) {
                    onlinePlayer.showPlayer(plugin, player);
                    player.showPlayer(plugin, onlinePlayer);
                } else {
                    onlinePlayer.hidePlayer(plugin, player);
                }
            }
        }
    }

    /**
     * Mark player as dead
     */
    public void markPlayerDead(Player player) {
        if (state != EventState.RUNNING && state != EventState.ENDING) return;
        
        alivePlayers.remove(player.getUniqueId());

        // Record death for placement tracking
        eventStatsManager.recordDeath(player.getUniqueId());

        if (plugin.getConfigManager().shouldLogPlayers()) {
            plugin.getLogger().info("[DEBUG:PLAYER] " + player.getName() + " marked as dead. Remaining alive: " + alivePlayers.size() +
                    " | Placement: #" + eventStatsManager.getPlacement(player.getUniqueId()));
        }
    }

    /**
     * Broadcast kill message with styled kill feed
     */
    public void broadcastKill(Player killer, Player victim, int killerRank) {
        if (state != EventState.RUNNING) return;

        // Use the KillFeedManager for styled messages
        KillFeedManager killFeedManager = plugin.getKillFeedManager();
        if (killFeedManager != null) {
            killFeedManager.broadcastKill(killer, victim, killerRank);
        } else {
            // Fallback to config message
            String message = plugin.getConfigManager().getMessage("kill-broadcast")
                    .replace("%killer%", killer.getName())
                    .replace("%rank%", String.valueOf(killerRank))
                    .replace("%victim%", victim.getName());
            Bukkit.broadcastMessage(MessageUtils.colorize(MessageUtils.PREFIX + message));
        }
    }

    // Getters and setters

    public boolean isEventRunning() {
        return state == EventState.RUNNING;
    }

    public EventState getState() {
        return state;
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

    public boolean isNaturalRegenAllowed() {
        return naturalRegenAllowed;
    }

    public void toggleNaturalRegen() {
        this.naturalRegenAllowed = !this.naturalRegenAllowed;
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

    /**
     * Get a copy of the joined players set
     */
    public Set<UUID> getJoinedPlayers() {
        return new HashSet<>(joinedPlayers);
    }

    /**
     * Get a copy of the alive players set (for auto-balancing)
     */
    public Set<UUID> getAlivePlayers() {
        return new HashSet<>(alivePlayers);
    }

    public int getAlivePlayerCount() {
        return alivePlayers.size();
    }

    public Set<UUID> getSpectators() {
        return new HashSet<>(spectators);
    }

    /**
     * Trigger auto-balance check for teams
     * Called after a player dies or disconnects
     */
    public void triggerAutoBalance() {
        if (state != EventState.RUNNING || teamSize <= 1) {
            return; // Only balance in team mode
        }

        if (plugin.getConfigManager().shouldLogTeams()) {
            plugin.getLogger().info("[DEBUG:TEAM] Checking if auto-balance is needed...");
        }

        if (teamManager.needsBalancing(alivePlayers)) {
            if (plugin.getConfigManager().shouldLogTeams()) {
                plugin.getLogger().info("[DEBUG:TEAM] Teams unbalanced, triggering auto-balance");
            }
            teamManager.autoBalanceTeams(alivePlayers);
        }
    }

    // ==================== Pending Respawn Tracking ====================

    public void markPendingRespawn(Player player) {
        pendingRespawn.add(player.getUniqueId());
    }

    public boolean isPendingRespawn(Player player) {
        return pendingRespawn.contains(player.getUniqueId());
    }

    public void clearPendingRespawn(Player player) {
        pendingRespawn.remove(player.getUniqueId());
    }

    // ==================== Fall Damage Immunity ====================

    public boolean hasFallDamageImmunity(Player player) {
        return fallDamageImmune.contains(player.getUniqueId());
    }

    // ==================== Arena Integration ====================

    public void setArenaManager(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    public void setArenaBoundaryListener(ArenaBoundaryListener arenaBoundaryListener) {
        this.arenaBoundaryListener = arenaBoundaryListener;
    }
}