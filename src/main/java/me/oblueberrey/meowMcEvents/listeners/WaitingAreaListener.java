package me.oblueberrey.meowMcEvents.listeners;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.utils.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * Blocks most events for players in the waiting area (joined but event not started)
 * Allows: chat messages, /leave command
 * Blocks: PvP, block break/place, item drops, inventory, interactions
 */
public class WaitingAreaListener implements Listener {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;

    public WaitingAreaListener(MeowMCEvents plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    /**
     * Check if player is in waiting area (joined but event not started)
     */
    private boolean isInWaitingArea(Player player) {
        return eventManager.isCountdownActive() && eventManager.hasPlayerJoined(player);
    }

    /**
     * Block block breaking in waiting area
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isInWaitingArea(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Block block placing in waiting area
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isInWaitingArea(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Block PvP damage in waiting area
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            if (isInWaitingArea(attacker)) {
                event.setCancelled(true);
                return;
            }
        }
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            if (isInWaitingArea(victim)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Block all damage in waiting area
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isInWaitingArea(player)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Block hunger in waiting area
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isInWaitingArea(player)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Block item drops in waiting area
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isInWaitingArea(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Block item pickups in waiting area
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (isInWaitingArea(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Block inventory clicks in waiting area
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (isInWaitingArea(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Block hand swap in waiting area
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSwapHands(PlayerSwapHandItemsEvent event) {
        if (isInWaitingArea(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Block interactions in waiting area
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isInWaitingArea(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
