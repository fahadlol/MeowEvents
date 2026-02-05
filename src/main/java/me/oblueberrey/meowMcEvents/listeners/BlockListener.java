package me.oblueberrey.meowMcEvents.listeners;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.utils.ConfigManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class BlockListener implements Listener {

    private final EventManager eventManager;

    public BlockListener(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    private void debug(String message) {
        MeowMCEvents plugin = MeowMCEvents.getInstance();
        if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG:BLOCK] " + message);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!eventManager.isEventRunning()) {
            return;
        }

        Player player = event.getPlayer();

        // Block spectators from building
        if (eventManager.isSpectator(player)) {
            event.setCancelled(true);
            return;
        }

        // Only apply to players in the event
        if (!eventManager.isPlayerInEvent(player)) {
            return;
        }

        // Block during grace period
        if (eventManager.isGracePeriodActive()) {
            event.setCancelled(true);
            player.sendMessage(ConfigManager.colorize("&#AAAAAA&#FF5555grace period active"));
            return;
        }

        // If building is not allowed, cancel the event
        if (!eventManager.isBuildingAllowed()) {
            event.setCancelled(true);
            player.sendMessage(ConfigManager.colorize("&#AAAAAA&#FF5555building disabled"));
            debug(player.getName() + " tried to place block but building is disabled");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!eventManager.isEventRunning()) {
            return;
        }

        Player player = event.getPlayer();

        // Block spectators from breaking
        if (eventManager.isSpectator(player)) {
            event.setCancelled(true);
            return;
        }

        // Only apply to players in the event
        if (!eventManager.isPlayerInEvent(player)) {
            return;
        }

        // Block during grace period
        if (eventManager.isGracePeriodActive()) {
            event.setCancelled(true);
            player.sendMessage(ConfigManager.colorize("&#AAAAAA&#FF5555grace period active"));
            return;
        }

        // If breaking is not allowed, cancel the event
        if (!eventManager.isBreakingAllowed()) {
            event.setCancelled(true);
            player.sendMessage(ConfigManager.colorize("&#AAAAAA&#FF5555breaking disabled"));
            debug(player.getName() + " tried to break block but breaking is disabled");
        }
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!eventManager.isEventRunning()) return;
        Player player = event.getPlayer();
        if (!eventManager.isPlayerInEvent(player) && !eventManager.isSpectator(player)) return;

        if (eventManager.isSpectator(player) || eventManager.isGracePeriodActive() || !eventManager.isBuildingAllowed()) {
            event.setCancelled(true);
            if (eventManager.isGracePeriodActive()) {
                player.sendMessage(ConfigManager.colorize("&#AAAAAA&#FF5555grace period active"));
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!eventManager.isEventRunning()) return;
        Player player = event.getPlayer();
        if (!eventManager.isPlayerInEvent(player) && !eventManager.isSpectator(player)) return;

        // Block Flint and Steel during grace period or if building is disabled
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getItem() != null && event.getItem().getType() == Material.FLINT_AND_STEEL) {
            if (eventManager.isSpectator(player) || eventManager.isGracePeriodActive() || !eventManager.isBuildingAllowed()) {
                event.setCancelled(true);
                if (eventManager.isGracePeriodActive()) {
                    player.sendMessage(ConfigManager.colorize("&#AAAAAA&#FF5555grace period active"));
                }
            }
        }
    }
}