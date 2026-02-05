package me.oblueberrey.meowMcEvents.managers;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.utils.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.UUID;

/**
 * Manages the Tab List (player list) formatting during events.
 * Shows team colors, alive/dead status, and spectator indicators.
 * 
 * NOTE: This works alongside ScoreboardManager by using the SAME scoreboard
 * that players already have assigned (from ScoreboardManager).
 * 
 * Format:
 * - Alive players: [TeamColor]PlayerName
 * - Dead/Spectators: [GRAY]☠ PlayerName
 * - Team mode: [TeamColor][T#] PlayerName
 */
public class TabListManager {

    private final MeowMCEvents plugin;
    private BukkitTask updateTask;
    private volatile boolean active = false;

    // Status symbols for header/footer
    private static final String SKULL = "\u2620";      // ☠ (dead/spectator)
    private static final String SWORD = "\u2694";      // ⚔ (alive)

    public TabListManager(MeowMCEvents plugin) {
        this.plugin = plugin;
    }

    /**
     * Start tab list formatting for event participants
     * Uses existing scoreboards from ScoreboardManager (doesn't create new ones)
     */
    public void startTabList(Set<UUID> players) {
        if (active) return;

        active = true;

        // Start update task (every 20 ticks = 1 second)
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateTabList, 0L, 20L);

        debug("Tab list started for event");
    }

    /**
     * Add a player to the tab list system
     */
    public void addPlayer(Player player) {
        // No-op - we use existing scoreboards from ScoreboardManager
    }

    /**
     * Stop tab list formatting
     */
    public void stopTabList() {
        if (!active) return;

        active = false;

        // Cancel update task
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        // Clear header/footer for all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearHeaderFooter(player);
        }

        debug("Tab list stopped");
    }

    /**
     * Update tab list for all players
     * Only updates header/footer - name colors are handled by ScoreboardManager
     */
    private void updateTabList() {
        if (!active) return;

        EventManager eventManager = plugin.getEventManager();
        TeamManager teamManager = plugin.getTeamManager();

        if (eventManager == null || !eventManager.isEventRunning()) {
            stopTabList();
            return;
        }

        boolean isTeamMode = teamManager != null && teamManager.isTeamMode();
        int aliveCount = eventManager.getAlivePlayerCount();
        int spectatorCount = eventManager.getSpectators().size();

        // Update header and footer for all online players
        updateHeaderFooter(aliveCount, spectatorCount, isTeamMode);
    }

    /**
     * Update tab list header and footer
     */
    private void updateHeaderFooter(int aliveCount, int spectatorCount, boolean isTeamMode) {
        String header = ConfigManager.colorize(
                "\n" +
                "&6&lMEOWMC EVENTS\n" +
                "&7━━━━━━━━━━━━━━━━━━━━━━━━\n"
        );

        String modeText = isTeamMode ? "&eTeam Mode" : "&eSolo Mode";
        String footer = ConfigManager.colorize(
                "\n&7━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "&a" + SWORD + " Alive: &f" + aliveCount + "  " +
                "&7" + SKULL + " Spectators: &f" + spectatorCount + "\n" +
                modeText + "\n"
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setPlayerListHeaderFooter(header, footer);
        }
    }

    /**
     * Clear header/footer for a player
     */
    public void clearHeaderFooter(Player player) {
        player.setPlayerListHeaderFooter("", "");
    }

    /**
     * Debug logging
     */
    private void debug(String message) {
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG:TABLIST] " + message);
        }
    }

    /**
     * Check if tab list is active
     */
    public boolean isActive() {
        return active;
    }
}
