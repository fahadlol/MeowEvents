package me.oblueberrey.meowMcEvents.gui;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.managers.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class KitsGUI implements Listener {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;
    private final KitManager kitManager;
    private final boolean returnToMainMenu;
    private static final String GUI_TITLE = ChatColor.DARK_PURPLE + "Select Event Kit";
    private static boolean listenerRegistered = false;

    public KitsGUI(MeowMCEvents plugin, EventManager eventManager) {
        this(plugin, eventManager, false);
    }

    public KitsGUI(MeowMCEvents plugin, EventManager eventManager, boolean returnToMainMenu) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.kitManager = plugin.getKitManager();
        this.returnToMainMenu = returnToMainMenu;
        // Only register listener once to prevent memory leak
        if (!listenerRegistered) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            listenerRegistered = true;
            debug("KitsGUI listener registered");
        }
    }

    private void debug(String message) {
        if (plugin.getConfigManager().shouldLogGui()) {
            plugin.getLogger().info("[DEBUG:GUI] " + message);
        }
    }

    public void openGUI(Player player) {
        debug(player.getName() + " opened Kit Selection GUI");
        List<String> kits = kitManager.getKitNames();

        int size = ((kits.size() + 8) / 9) * 9;
        if (size > 54) size = 54;
        if (size < 9) size = 9;

        Inventory gui = Bukkit.createInventory(null, size, GUI_TITLE);

        String currentKit = kitManager.getSelectedKit();

        int slot = 0;
        for (String kitName : kits) {
            if (slot >= 54) break;

            boolean isSelected = kitName.equals(currentKit);

            ItemStack item;
            ItemMeta meta;

            if (isSelected) {
                // Green wool for selected kit
                item = new ItemStack(Material.LIME_WOOL);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + kitName);

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GREEN + "✓ Currently Selected");
                lore.add("");
                lore.add(ChatColor.GRAY + "This kit will be given to");
                lore.add(ChatColor.GRAY + "all players when event starts");
                meta.setLore(lore);
            } else {
                // Grey wool for unselected kits
                item = new ItemStack(Material.GRAY_WOOL);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + kitName);

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Click to select this kit");
                lore.add("");
                lore.add(ChatColor.GRAY + "This kit will be given to");
                lore.add(ChatColor.GRAY + "all players when event starts");
                meta.setLore(lore);
            }

            item.setItemMeta(meta);
            gui.setItem(slot, item);
            slot++;
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        String title = event.getView().getTitle();
        if (!title.equals(GUI_TITLE)) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();

        if (!player.hasPermission("meowevent.admin")) {
            player.sendMessage(ChatColor.RED + "You need admin permission to select kits!");
            return;
        }

        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (!clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;

        // Only allow clicking grey wool (unselected kits)
        if (clicked.getType() == Material.LIME_WOOL) {
            player.sendMessage(ChatColor.YELLOW + "This kit is already selected!");
            return;
        }

        if (clicked.getType() != Material.GRAY_WOOL) {
            return;
        }

        // Get kit name from clicked item
        String kitName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        debug(player.getName() + " selected kit: " + kitName);

        // Set selected kit
        kitManager.setSelectedKit(kitName);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfigManager().getMessage("kit-selected").replace("%kit%", kitName)));

        player.closeInventory();

        // Return to main menu if opened from main GUI
        if (returnToMainMenu) {
            debug("Returning to main menu after kit selection");
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                EventGUI mainGUI = new EventGUI(plugin, eventManager);
                mainGUI.openGUI(player);
            }, 2L);
        }
    }
}