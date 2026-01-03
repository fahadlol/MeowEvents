package me.oblueberrey.meowMcEvents.listeners;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.managers.RankManager;
import org.bukkit.Bukkit;
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

        debug(victim.getName() + " died in event. Killer: " + (killer != null ? killer.getName() : "none"));

        // Handle killer rank increment
        if (killer != null && killer != victim && eventManager.isPlayerInEvent(killer)) {
            rankManager.incrementRank(killer);
            int killerRank = rankManager.getRank(killer);
            debug("Killer " + killer.getName() + " rank increased to " + killerRank);

            // Broadcast kill message with rank
            eventManager.broadcastKill(killer, victim, killerRank);
        }

        // Reset victim's rank to 0
        rankManager.resetRank(victim);

        // Mark player as dead in event
        eventManager.markPlayerDead(victim);

        // Teleport to player spawn after 3 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (victim.isOnline()) {
                Location playerSpawn = plugin.getConfigManager().getPlayerSpawnLocation();
                if (playerSpawn != null && playerSpawn.getWorld() != null) {
                    victim.teleport(playerSpawn);
                    debug(victim.getName() + " teleported to player spawn");
                } else {
                    plugin.getLogger().warning("Player spawn location is null - cannot teleport " + victim.getName());
                }
                victim.getInventory().clear();
                victim.setHealth(20.0);
                victim.setFoodLevel(20);
            } else {
                debug(victim.getName() + " is offline - skipping teleport");
            }
        }, 60L); // 3 seconds

        // Check for winner after death
        eventManager.checkForWinner();
    }
}