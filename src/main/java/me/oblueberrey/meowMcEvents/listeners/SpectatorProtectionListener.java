package me.oblueberrey.meowMcEvents.listeners;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * Protects spectators from unwanted interactions and blocks them from affecting the game.
 * Also prevents event players from dropping items (losing their kit).
 */
public class SpectatorProtectionListener implements Listener {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;

    public SpectatorProtectionListener(MeowMCEvents plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    // ==================== Spectator Protections ====================

    /**
     * Block spectators from taking any damage
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (eventManager.isSpectator(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Block spectators from dealing damage to players
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player attacker = (Player) event.getDamager();

        if (eventManager.isSpectator(attacker)) {
            event.setCancelled(true);
        }
    }

    /**
     * Block spectators from picking up items
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (eventManager.isSpectator(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Block spectators from dropping items
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorDrop(PlayerDropItemEvent event) {
        if (eventManager.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Block spectators from breaking blocks
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorBreak(BlockBreakEvent event) {
        if (eventManager.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Block spectators from placing blocks
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorPlace(BlockPlaceEvent event) {
        if (eventManager.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Block spectators from interacting with blocks (chests, doors, buttons, etc.)
     * Only allow right-click air with compass or leave dye
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorInteract(PlayerInteractEvent event) {
        if (!eventManager.isSpectator(event.getPlayer())) return;

        // Allow compass and leave dye clicks only for AIR interactions (no block interaction)
        if (event.getItem() != null) {
            Material type = event.getItem().getType();
            if (type == Material.COMPASS || type == Material.RED_DYE) {
                // Only allow if NOT interacting with a block
                Action action = event.getAction();
                if (action == Action.RIGHT_CLICK_AIR || action == Action.LEFT_CLICK_AIR) {
                    return; // Allow air clicks for compass/dye
                }
            }
        }
        
        // Block ALL other interactions
        event.setCancelled(true);
    }

    /**
     * Block spectators from interacting with entities
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorInteractEntity(PlayerInteractEntityEvent event) {
        if (eventManager.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Block spectator inventory changes (except hotbar for compass/dye)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorInventory(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (eventManager.isSpectator(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Block spectator hunger drain
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (eventManager.isSpectator(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Block spectators from opening containers (chests, barrels, etc.)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorOpenInventory(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        if (eventManager.isSpectator(player)) {
            // Allow opening only if it's NOT a container (allow spectator GUI)
            InventoryHolder holder = event.getInventory().getHolder();
            if (holder != null && !(holder instanceof Player)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Block spectators from using buckets
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorBucketEmpty(PlayerBucketEmptyEvent event) {
        if (eventManager.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorBucketFill(PlayerBucketFillEvent event) {
        if (eventManager.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Block spectators from entering beds
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorBedEnter(PlayerBedEnterEvent event) {
        if (eventManager.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Block spectators from entering vehicles
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player)) return;
        Player player = (Player) event.getEntered();

        if (eventManager.isSpectator(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Block spectators from damaging vehicles
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorVehicleDamage(VehicleDamageEvent event) {
        if (!(event.getAttacker() instanceof Player)) return;
        Player player = (Player) event.getAttacker();

        if (eventManager.isSpectator(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Block spectators from shearing entities
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorShear(PlayerShearEntityEvent event) {
        if (eventManager.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Block spectators from fishing
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorFish(PlayerFishEvent event) {
        if (eventManager.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Block spectators from manipulating armor stands
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorArmorStand(PlayerArmorStandManipulateEvent event) {
        if (eventManager.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Block spectators from taking lectern books
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorLecternBook(PlayerTakeLecternBookEvent event) {
        if (eventManager.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Block spectators from breaking hanging entities (paintings, item frames)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorHangingBreak(HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player)) return;
        Player player = (Player) event.getRemover();

        if (eventManager.isSpectator(player)) {
            event.setCancelled(true);
        }
    }

    // ==================== Spectator Visibility ====================

    /**
     * Hide all spectators from players who join during the event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joiningPlayer = event.getPlayer();
        
        // Hide all spectators from the joining player (unless they're also a spectator)
        for (UUID spectatorUUID : eventManager.getSpectators()) {
            Player spectator = plugin.getServer().getPlayer(spectatorUUID);
            if (spectator != null && spectator.isOnline() && !spectator.equals(joiningPlayer)) {
                joiningPlayer.hidePlayer(plugin, spectator);
            }
        }
    }

    // ==================== Event Player Protections ====================

    /**
     * Block fall damage for players with temporary immunity (after teleport)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (eventManager.hasFallDamageImmunity(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Block event players from dropping items (prevent losing kit)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEventPlayerDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        // Block drops for alive event players
        if (eventManager.isPlayerInEvent(player)) {
            event.setCancelled(true);
            return;
        }

        // Block drops for waiting players
        if (eventManager.isCountdownActive() && eventManager.hasPlayerJoined(player)) {
            event.setCancelled(true);
        }
    }
}
