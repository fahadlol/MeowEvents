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
    private static final String GUI_TITLE = ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Kit Selection";
    private static boolean listenerRegistered = false;

    // Kit icon materials for variety
    private static final Material[] KIT_ICONS = {
            Material.DIAMOND_SWORD,
            Material.IRON_CHESTPLATE,
            Material.GOLDEN_APPLE,
            Material.BOW,
            Material.SHIELD,
            Material.CROSSBOW,
            Material.TRIDENT,
            Material.NETHERITE_AXE,
            Material.ENCHANTED_GOLDEN_APPLE
    };

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

        // Calculate GUI size - 27 (3 rows) minimum, expand if needed
        int kitSlots = kits.size();
        int rows = Math.max(3, (int) Math.ceil((kitSlots + 9) / 7.0) + 2); // Add space for borders
        if (rows > 6) rows = 6;
        int size = rows * 9;

        Inventory gui = Bukkit.createInventory(null, size, GUI_TITLE);

        String currentKit = kitManager.getSelectedKit();

        // Fill border with colored glass
        fillBorder(gui, size);

        // Add header info item
        gui.setItem(4, createInfoItem(currentKit, kits.size()));

        // Place kits in center slots (avoiding borders)
        int[] kitSlotPositions = getKitSlots(size);
        int kitIndex = 0;

        for (int slot : kitSlotPositions) {
            if (kitIndex >= kits.size()) break;

            String kitName = kits.get(kitIndex);
            boolean isSelected = kitName.equals(currentKit);
            Material icon = KIT_ICONS[kitIndex % KIT_ICONS.length];

            gui.setItem(slot, createKitItem(kitName, isSelected, icon, kitIndex + 1));
            kitIndex++;
        }

        // Add back button if returning to main menu
        if (returnToMainMenu) {
            gui.setItem(size - 5, createBackButton());
        }

        player.openInventory(gui);
    }

    private ItemStack createInfoItem(String selectedKit, int totalKits) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Kit Information");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Total kits: " + ChatColor.WHITE + totalKits);
        lore.add(ChatColor.GRAY + "Selected: " + ChatColor.GREEN + selectedKit);
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click a kit below to select it");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createKitItem(String kitName, boolean isSelected, Material icon, int kitNumber) {
        ItemStack item = new ItemStack(isSelected ? Material.LIME_STAINED_GLASS_PANE : icon);
        ItemMeta meta = item.getItemMeta();

        if (isSelected) {
            meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "✓ " + kitName + " ✓");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "SELECTED");
            lore.add("");
            lore.add(ChatColor.GRAY + "This kit will be given to");
            lore.add(ChatColor.GRAY + "all players when event starts");
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "Kit #" + kitNumber);

            meta.setLore(lore);
        } else {
            meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + kitName);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Click to select this kit");
            lore.add("");
            lore.add(ChatColor.YELLOW + "➤ Click to select");
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "Kit #" + kitNumber);

            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "← Back");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Return to main menu");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void fillBorder(Inventory gui, int size) {
        ItemStack borderPane = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = borderPane.getItemMeta();
        meta.setDisplayName(" ");
        borderPane.setItemMeta(meta);

        ItemStack accentPane = new ItemStack(Material.MAGENTA_STAINED_GLASS_PANE);
        ItemMeta accentMeta = accentPane.getItemMeta();
        accentMeta.setDisplayName(" ");
        accentPane.setItemMeta(accentMeta);

        int rows = size / 9;

        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;

            // Top and bottom rows
            if (row == 0 || row == rows - 1) {
                // Use accent color for corners
                if (col == 0 || col == 8) {
                    gui.setItem(i, accentPane.clone());
                } else if (i != 4) { // Don't fill info slot
                    gui.setItem(i, borderPane.clone());
                }
            }
            // Left and right columns
            else if (col == 0 || col == 8) {
                gui.setItem(i, borderPane.clone());
            }
        }
    }

    private int[] getKitSlots(int size) {
        // Return center slots based on GUI size
        if (size == 27) {
            return new int[]{10, 11, 12, 13, 14, 15, 16};
        } else if (size == 36) {
            return new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        } else if (size == 45) {
            return new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        } else {
            return new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        }
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

        Material type = clicked.getType();

        // Handle back button
        if (type == Material.ARROW) {
            String displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            if (displayName.contains("Back")) {
                debug(player.getName() + " clicked back button");
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    EventGUI mainGUI = new EventGUI(plugin, eventManager);
                    mainGUI.openGUI(player);
                }, 2L);
                return;
            }
        }

        // Ignore border glass panes, info book, and already selected kit
        if (type == Material.PURPLE_STAINED_GLASS_PANE ||
            type == Material.MAGENTA_STAINED_GLASS_PANE ||
            type == Material.BOOK ||
            type == Material.LIME_STAINED_GLASS_PANE) {

            if (type == Material.LIME_STAINED_GLASS_PANE) {
                player.sendMessage(ChatColor.YELLOW + "This kit is already selected!");
            }
            return;
        }

        // Check if it's a kit icon
        boolean isKitIcon = false;
        for (Material kitIcon : KIT_ICONS) {
            if (type == kitIcon) {
                isKitIcon = true;
                break;
            }
        }

        if (!isKitIcon) {
            return;
        }

        // Get kit name from clicked item (remove formatting)
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