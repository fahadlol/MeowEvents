package me.oblueberrey.meowMcEvents.listeners;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.managers.TeamManager;
import me.oblueberrey.meowMcEvents.utils.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpectatorCompassListener implements Listener {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;
    private final TeamManager teamManager;

    // Improved RGB Colors
    private static final String GREY = "&#AAAAAA";
    private static final String YELLOW = "&#FFE566";
    private static final String ORANGE = "&#FF9944";
    private static final String RED = "&#FF5555";
    private static final String GREEN = "&#55FF55";
    private static final String AQUA = "&#55FFFF";

    private static final String GUI_TITLE = ConfigManager.colorize("&#666666\uD83D\uDC41 " + YELLOW + "Spectate Player");
    private static final String COMPASS_NAME = ConfigManager.colorize("&#666666\uD83E\uDDED " + YELLOW + "Player Tracker");
    private static final String LEAVE_DYE_NAME = ConfigManager.colorize("&#666666\u2716 " + RED + "Leave Event");

    // Thread-safe map to prevent ConcurrentModificationException
    private final Map<UUID, Integer> spectatorIndex = new ConcurrentHashMap<>();

    public SpectatorCompassListener(MeowMCEvents plugin, EventManager eventManager, TeamManager teamManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.teamManager = teamManager;
    }

    /**
     * Create the spectator compass item
     */
    public static ItemStack createSpectatorCompass() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(COMPASS_NAME);
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ConfigManager.colorize(GREY + "track alive players"));
            lore.add("");
            lore.add(ConfigManager.colorize(ORANGE + "click " + GREY + "-open menu"));
            lore.add(ConfigManager.colorize(ORANGE + "shift+right " + GREY + "-next player"));
            lore.add(ConfigManager.colorize(ORANGE + "shift+left " + GREY + "-previous"));
            meta.setLore(lore);
            compass.setItemMeta(meta);
        }
        return compass;
    }

    /**
     * Create the leave event red dye item
     */
    public static ItemStack createLeaveDye() {
        ItemStack dye = new ItemStack(Material.RED_DYE);
        ItemMeta meta = dye.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(LEAVE_DYE_NAME);
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ConfigManager.colorize(GREY + "click to leave the event"));
            lore.add("");
            lore.add(ConfigManager.colorize(RED + "click " + GREY + "-/leave"));
            meta.setLore(lore);
            dye.setItemMeta(meta);
        }
        return dye;
    }

    @EventHandler
    public void onSpectatorInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Check if player is a spectator
        if (!eventManager.isSpectator(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        event.setCancelled(true);

        Action action = event.getAction();

        // Handle compass
        if (item.getType() == Material.COMPASS && COMPASS_NAME.equals(meta.getDisplayName())) {
            if (player.isSneaking()) {
                // Shift+click: cycle players
                if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                    cycleToNextPlayer(player);
                } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                    cycleToPreviousPlayer(player);
                }
            } else {
                // Normal click (left or right): open GUI
                openSpectatorGUI(player);
            }
        }
        // Handle red dye - leave event
        else if (item.getType() == Material.RED_DYE && LEAVE_DYE_NAME.equals(meta.getDisplayName())) {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK ||
                action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                // Execute /leave command
                player.performCommand("leave");
            }
        }
    }

    /**
     * Lock spectator inventory - prevent clicking
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // Check if spectator - lock their inventory
        if (eventManager.isSpectator(player)) {
            // Allow closing the spectator GUI
            String title = event.getView().getTitle();
            if (title.equals(GUI_TITLE)) {
                // Allow clicking in spectator GUI to teleport
                return;
            }
            // Block all other inventory interactions
            event.setCancelled(true);
        }
    }

    /**
     * Lock spectator inventory - prevent dragging
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (eventManager.isSpectator(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent spectators from dropping items
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (eventManager.isSpectator(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent spectators from swapping hands
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (eventManager.isSpectator(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Make spectators immune to ALL damage
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (eventManager.isSpectator(player)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Prevent spectators from dealing damage to others
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (eventManager.isSpectator(player)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Prevent spectators from picking up items
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (eventManager.isSpectator(player)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Prevent hunger for spectators
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (eventManager.isSpectator(player)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Prevent mobs from targeting spectators
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMobTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player) {
            Player player = (Player) event.getTarget();
            if (eventManager.isSpectator(player)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Prevent spectators from interacting with entities
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorInteractEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        if (eventManager.isSpectator(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Open the spectator teleport GUI
     */
    private void openSpectatorGUI(Player spectator) {
        List<Player> alivePlayers = getAlivePlayers();

        if (alivePlayers.isEmpty()) {
            spectator.sendMessage(ChatColor.RED + "No players alive to spectate!");
            return;
        }

        int size = Math.min(54, ((alivePlayers.size() + 8) / 9) * 9);
        if (size < 9) size = 9;

        Inventory gui = Bukkit.createInventory(null, size, GUI_TITLE);

        for (int i = 0; i < alivePlayers.size() && i < 54; i++) {
            Player target = alivePlayers.get(i);
            gui.setItem(i, createPlayerHead(target));
        }

        spectator.openInventory(gui);
        spectator.playSound(spectator.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    /**
     * Create a player head item for the GUI
     */
    private ItemStack createPlayerHead(Player target) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta != null) {
            meta.setOwningPlayer(target);

            // Color name based on team
            int team = teamManager.getTeam(target);
            ChatColor teamColor = team != -1 ? teamManager.getTeamColor(team) : ChatColor.WHITE;
            meta.setDisplayName(teamColor + target.getName());

            List<String> lore = new ArrayList<>();
            lore.add("");

            // Show team info if in team mode
            if (team != -1) {
                lore.add(ChatColor.GRAY + "Team: " + teamColor + "Team " + team);
            }

            // Show health
            double health = target.getHealth();
            double maxHealth = target.getMaxHealth();
            int healthBars = (int) ((health / maxHealth) * 10);
            StringBuilder healthBar = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                if (i < healthBars) {
                    healthBar.append(ChatColor.RED + "|");
                } else {
                    healthBar.append(ChatColor.DARK_GRAY + "|");
                }
            }
            lore.add(ChatColor.GRAY + "Health: " + healthBar.toString() + ChatColor.WHITE + " " + String.format("%.1f", health));

            // Show distance
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to teleport!");

            meta.setLore(lore);
            skull.setItemMeta(meta);
        }

        return skull;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        String title = event.getView().getTitle();
        if (!title.equals(GUI_TITLE)) return;

        event.setCancelled(true);

        Player spectator = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;

        // SECURITY: Check instanceof before casting to prevent ClassCastException
        if (!(clicked.getItemMeta() instanceof SkullMeta)) return;
        SkullMeta meta = (SkullMeta) clicked.getItemMeta();
        if (meta.getOwningPlayer() == null) return;

        Player target = meta.getOwningPlayer().getPlayer();
        if (target == null || !target.isOnline()) {
            spectator.sendMessage(ChatColor.RED + "Player is no longer online!");
            spectator.closeInventory();
            return;
        }

        // Teleport spectator to target
        teleportToPlayer(spectator, target);
        spectator.closeInventory();
    }

    /**
     * Cycle to the next alive player
     * Thread-safe implementation with proper bounds validation
     */
    private void cycleToNextPlayer(Player spectator) {
        List<Player> alivePlayers = getAlivePlayers();

        // SECURITY: Check size to prevent division by zero
        int size = alivePlayers.size();
        if (size == 0) {
            spectator.sendMessage(ChatColor.RED + "No players alive to spectate!");
            spectatorIndex.remove(spectator.getUniqueId()); // Reset invalid index
            return;
        }

        int currentIndex = spectatorIndex.getOrDefault(spectator.getUniqueId(), -1);
        
        // Validate current index is still valid for the current list size
        if (currentIndex >= size) {
            currentIndex = -1; // Reset if out of bounds
        }
        
        int nextIndex = (currentIndex + 1) % size;
        
        // Double-check bounds after calculation (defensive)
        if (nextIndex < 0 || nextIndex >= size) {
            nextIndex = 0;
        }
        
        spectatorIndex.put(spectator.getUniqueId(), nextIndex);

        Player target = alivePlayers.get(nextIndex);
        if (target != null && target.isOnline()) {
            teleportToPlayer(spectator, target);
        } else {
            // Target went offline between list fetch and teleport, try again
            spectator.sendMessage(ChatColor.YELLOW + "Player went offline, trying next...");
            cycleToNextPlayer(spectator);
        }
    }

    /**
     * Cycle to the previous alive player
     * Thread-safe implementation with proper bounds validation
     */
    private void cycleToPreviousPlayer(Player spectator) {
        List<Player> alivePlayers = getAlivePlayers();

        // SECURITY: Check size to prevent division by zero
        int size = alivePlayers.size();
        if (size == 0) {
            spectator.sendMessage(ChatColor.RED + "No players alive to spectate!");
            spectatorIndex.remove(spectator.getUniqueId()); // Reset invalid index
            return;
        }

        int currentIndex = spectatorIndex.getOrDefault(spectator.getUniqueId(), 0);
        
        // Validate current index is still valid for the current list size
        if (currentIndex >= size || currentIndex < 0) {
            currentIndex = 0; // Reset if out of bounds
        }
        
        int prevIndex = (currentIndex - 1 + size) % size;
        
        // Double-check bounds after calculation (defensive)
        if (prevIndex < 0 || prevIndex >= size) {
            prevIndex = 0;
        }
        
        spectatorIndex.put(spectator.getUniqueId(), prevIndex);

        Player target = alivePlayers.get(prevIndex);
        if (target != null && target.isOnline()) {
            teleportToPlayer(spectator, target);
        } else {
            // Target went offline between list fetch and teleport, try again
            spectator.sendMessage(ChatColor.YELLOW + "Player went offline, trying previous...");
            cycleToPreviousPlayer(spectator);
        }
    }

    /**
     * Teleport spectator to a target player
     */
    private void teleportToPlayer(Player spectator, Player target) {
        spectator.teleport(target.getLocation());
        spectator.playSound(spectator.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.0f);

        // Show team color in message
        int team = teamManager.getTeam(target);
        ChatColor teamColor = team != -1 ? teamManager.getTeamColor(team) : ChatColor.WHITE;

        spectator.sendMessage(ChatColor.GRAY + "Now spectating: " + teamColor + target.getName() +
                ChatColor.GRAY + " | " + ChatColor.RED + String.format("%.1f", target.getHealth()) + " HP");
    }

    /**
     * Get list of alive players sorted by name
     */
    private List<Player> getAlivePlayers() {
        List<Player> players = new ArrayList<>();
        for (UUID uuid : eventManager.getAlivePlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        players.sort(Comparator.comparing(Player::getName));
        return players;
    }

    /**
     * Clear spectator index when they leave spectator mode
     */
    public void clearSpectatorIndex(Player player) {
        spectatorIndex.remove(player.getUniqueId());
    }
}
