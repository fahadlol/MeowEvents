package me.oblueberrey.meowMcEvents.listeners;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.managers.EventStatsManager;
import me.oblueberrey.meowMcEvents.managers.RankManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;
    private final RankManager rankManager;

    public PlayerDeathListener(MeowMCEvents plugin, EventManager eventManager, RankManager rankManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.rankManager = rankManager;
    }

    private void debug(String message) {
        if (plugin.getConfigManager().shouldLogPlayers()) {
            plugin.getLogger().info("[DEBUG:DEATH] " + message);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!eventManager.isEventRunning()) {
            return;
        }

        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Check if victim is in the event
        if (!eventManager.isPlayerInEvent(victim)) {
            debug(victim.getName() + " died but is not in event - ignoring");
            return;
        }

        // SECURITY: Clear drops immediately to prevent item duplication exploit
        // This prevents players from picking up their items before spectator mode
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Also clear the player's inventory immediately
        victim.getInventory().clear();

        debug(victim.getName() + " died in event. Killer: " + (killer != null ? killer.getName() : "none"));

        // Handle killer rank increment and stats
        if (killer != null && killer != victim && eventManager.isPlayerInEvent(killer)) {
            rankManager.incrementRank(killer);
            int killerRank = rankManager.getRank(killer);

            // Record kill for stats
            EventStatsManager statsManager = plugin.getEventStatsManager();
            if (statsManager != null) {
                statsManager.recordKill(killer.getUniqueId());
            }

            debug("Killer " + killer.getName() + " rank increased to " + killerRank +
                    " | Total kills: " + (statsManager != null ? statsManager.getKills(killer.getUniqueId()) : killerRank));

            // Broadcast kill message with rank
            eventManager.broadcastKill(killer, victim, killerRank);
        }

        // Reset victim's rank to 0
        rankManager.resetRank(victim);

        // Mark player as dead in event
        eventManager.markPlayerDead(victim);

        // Set player as spectator after 3 seconds (so they can see their death)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (victim.isOnline()) {
                // Add as spectator (gives compass automatically)
                eventManager.addSpectator(victim);
                victim.setHealth(20.0);
                victim.setFoodLevel(20);
                victim.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        "&6&l[MeowEvent] &eYou are now spectating! Use your &bcompass &eto track players or &c/leave &eto exit."));
                debug(victim.getName() + " set to spectator mode with compass");
            } else {
                debug(victim.getName() + " is offline - skipping spectator mode");
            }
        }, 60L); // 3 seconds

        // Check for winner after death
        eventManager.checkForWinner();

        // Trigger auto-balance after death (with delay)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (eventManager.isEventRunning()) {
                eventManager.triggerAutoBalance();
            }
        }, 80L); // 4 seconds delay (after spectator setup)
    }
}