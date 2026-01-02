package me.oblueberrey.meowMcEvents.listeners;

import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.managers.RankManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final EventManager eventManager;
    private final RankManager rankManager;

    public PlayerDeathListener(EventManager eventManager, RankManager rankManager) {
        this.eventManager = eventManager;
        this.rankManager = rankManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!eventManager.isEventRunning()) {
            return;
        }

        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Handle killer rank increment
        if (killer != null && killer != victim) {
            rankManager.incrementRank(killer);
            int killerRank = rankManager.getRank(killer);

            // Broadcast kill message with rank
            eventManager.broadcastKill(killer, victim, killerRank);
        }

        // Reset victim's rank to 0
        rankManager.resetRank(victim);

        // Mark player as dead in event
        eventManager.markPlayerDead(victim);

        // Check for winner after death
        eventManager.checkForWinner();
    }
}