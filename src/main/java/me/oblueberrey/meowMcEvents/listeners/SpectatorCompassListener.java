package me.oblueberrey.meowMcEvents.listeners;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.managers.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class SpectatorCompassListener implements Listener {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;
    private final TeamManager teamManager;

    private static final String GUI_TITLE = ChatColor.GOLD + "Spectate Player";
    private static final String COMPASS_NAME = ChatColor.AQUA + "" + ChatColor.BOLD + "Player Tracker";

    // Track current spectating target for cycling
    private final Map<UUID, Integer> spectatorIndex = new HashMap<>();

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
            lore.add(ChatColor.GRAY + "Track alive players in the event!");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Left-Click" + ChatColor.GRAY + " - Open player menu");
            lore.add(ChatColor.YELLOW + "Right-Click" + ChatColor.GRAY + " - Cycle to next player");
            lore.add(ChatColor.YELLOW + "Shift+Right" + ChatColor.GRAY + " - Cycle to previous");
            meta.setLore(lore);
            compass.setItemMeta(meta);
        }
        return compass;
    }

    @EventHandler
    public void onCompassClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Check if player is a spectator
        if (!eventManager.isSpectator(player)) return;

        // Check if holding compass
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.COMPASS) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !COMPASS_NAME.equals(meta.getDisplayName())) return;

        event.setCancelled(true);

        Action action = event.getAction();

        // Left click - Open GUI
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            openSpectatorGUI(player);
        }
        // Right click - Cycle players
        else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (player.isSneaking()) {
                cycleToPreviousPlayer(player);
            } else {
                cycleToNextPlayer(player);
            }
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
     */
    private void cycleToNextPlayer(Player spectator) {
        List<Player> alivePlayers = getAlivePlayers();

        // SECURITY: Check size to prevent division by zero
        int size = alivePlayers.size();
        if (size == 0) {
            spectator.sendMessage(ChatColor.RED + "No players alive to spectate!");
            return;
        }

        int currentIndex = spectatorIndex.getOrDefault(spectator.getUniqueId(), -1);
        int nextIndex = (currentIndex + 1) % size;
        spectatorIndex.put(spectator.getUniqueId(), nextIndex);

        // Bounds check in case list changed
        if (nextIndex >= alivePlayers.size()) {
            nextIndex = 0;
        }

        Player target = alivePlayers.get(nextIndex);
        teleportToPlayer(spectator, target);
    }

    /**
     * Cycle to the previous alive player
     */
    private void cycleToPreviousPlayer(Player spectator) {
        List<Player> alivePlayers = getAlivePlayers();

        // SECURITY: Check size to prevent division by zero
        int size = alivePlayers.size();
        if (size == 0) {
            spectator.sendMessage(ChatColor.RED + "No players alive to spectate!");
            return;
        }

        int currentIndex = spectatorIndex.getOrDefault(spectator.getUniqueId(), 0);
        int prevIndex = (currentIndex - 1 + size) % size;
        spectatorIndex.put(spectator.getUniqueId(), prevIndex);

        // Bounds check in case list changed
        if (prevIndex >= alivePlayers.size()) {
            prevIndex = 0;
        }

        Player target = alivePlayers.get(prevIndex);
        teleportToPlayer(spectator, target);
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

        spectator.sendMessage(ChatColor.GRAY + "Now spectating: " + teamColor + target.getName());

        // Send action bar
        spectator.sendActionBar(ChatColor.AQUA + "Spectating: " + teamColor + target.getName() +
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
