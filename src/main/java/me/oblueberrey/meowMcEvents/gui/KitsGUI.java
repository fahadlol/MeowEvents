package me.oblueberrey.meowMcEvents.gui;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.managers.KitManager;
import me.oblueberrey.meowMcEvents.utils.ButtonBuilder;
import me.oblueberrey.meowMcEvents.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class KitsGUI implements Listener {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;
    private final KitManager kitManager;
    private final boolean returnToMainMenu;

    // Improved RGB Colors
    private static final String GREY = "&#AAAAAA";
    private static final String YELLOW = "&#FFE566";
    private static final String ORANGE = "&#FF9944";
    private static final String RED = "&#FF5555";
    private static final String GREEN = "&#55FF55";
    private static final String GOLD = "&#FFE566";
    private static final String AQUA = "&#55FFFF";
    private static final String PINK = "&#FF7EB3";

    private static final String GUI_TITLE = MessageUtils.colorize("&#666666\uD83D\uDCE6 " + MessageUtils.gradient("Kit Repository", "#FF7EB3", "#FF9944", "#FFE566"));
    private static final AtomicBoolean listenerRegistered = new AtomicBoolean(false);

    // Kit wool materials: grey for unselected, green for selected
    private static final Material KIT_UNSELECTED = Material.GRAY_WOOL;
    private static final Material KIT_SELECTED = Material.LIME_WOOL;

    public KitsGUI(MeowMCEvents plugin, EventManager eventManager) {
        this(plugin, eventManager, false);
    }

    public KitsGUI(MeowMCEvents plugin, EventManager eventManager, boolean returnToMainMenu) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.kitManager = plugin.getKitManager();
        this.returnToMainMenu = returnToMainMenu;
        // Only register listener once to prevent memory leak (thread-safe)
        if (listenerRegistered.compareAndSet(false, true)) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
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

        // Calculate GUI size - 54 slots for a spacious feel
        int size = 54;
        Inventory gui = Bukkit.createInventory(null, size, GUI_TITLE);

        String currentKit = kitManager.getSelectedKit();

        // Decorative Border
        fillBorder(gui);

        // Add header info item
        gui.setItem(4, createInfoItem(currentKit, kits.size()));

        // Place kits in center slots (avoiding borders)
        int[] kitSlotPositions = getKitSlots();
        int kitIndex = 0;

        for (int slot : kitSlotPositions) {
            if (kitIndex >= kits.size()) break;

            String kitName = kits.get(kitIndex);
            boolean isSelected = kitName.equals(currentKit);

            gui.setItem(slot, createKitItem(kitName, isSelected, kitIndex + 1));
            kitIndex++;
        }

        // Add back button
        gui.setItem(49, createBackButton());

        player.openInventory(gui);
    }

    private ItemStack createInfoItem(String selectedKit, int totalKits) {
        return new ButtonBuilder(Material.BOOK)
                .name(GOLD + "Kit information")
                .lore(GREY + "Available kits: " + YELLOW + totalKits,
                      GREY + "Currently active: " + GREEN + selectedKit,
                      "",
                      GREY + "Select a kit from the collection below.")
                .build();
    }

    private ItemStack createKitItem(String kitName, boolean isSelected, int kitNumber) {
        ButtonBuilder builder = new ButtonBuilder(isSelected ? KIT_SELECTED : KIT_UNSELECTED);
        
        if (isSelected) {
            builder.name(GREEN + "\u2713 " + kitName)
                   .lore(GREY + "This kit is currently selected.",
                         GREY + "It will be distributed to all",
                         GREY + "players upon event start.",
                         "",
                         GREEN + "Active kit #" + kitNumber)
                   .glow(true);
        } else {
            builder.name(YELLOW + kitName)
                   .lore(GREY + "Click to set this as the active kit.",
                         "",
                         GOLD + "Click to select.",
                         "",
                         GREY + "Kit #" + kitNumber);
        }

        return builder.build();
    }

    private ItemStack createBackButton() {
        return new ButtonBuilder(Material.ARROW)
                .name(RED + "Back")
                .lore(GREY + "Return to administration menu")
                .build();
    }

    private void fillBorder(Inventory gui) {
        ItemStack border = new ButtonBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .build();
        ItemStack corner = new ButtonBuilder(Material.ORANGE_STAINED_GLASS_PANE)
                .name(" ")
                .build();

        for (int i = 0; i < gui.getSize(); i++) {
            int row = i / 9;
            int col = i % 9;

            if (row == 0 || row == 5 || col == 0 || col == 8) {
                if (i == 0 || i == 8 || i == 45 || i == 53) {
                    gui.setItem(i, corner);
                } else if (i != 4 && i != 49) { // Don't block info and back button
                    gui.setItem(i, border);
                }
            }
        }
    }

    private int[] getKitSlots() {
        // center 28 slots (rows 1-4, cols 1-7)
        return new int[]{
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        };
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        String title = event.getView().getTitle();
        if (!title.equals(GUI_TITLE)) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();

        if (!player.hasPermission("meowevent.admin")) {
            player.sendMessage(MessageUtils.format(RED + "no permission"));
            return;
        }

        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        Material type = clicked.getType();

        // Handle back button
        if (event.getSlot() == 49) {
            debug(player.getName() + " clicked back button");
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                EventGUI mainGUI = new EventGUI(plugin, eventManager);
                mainGUI.openGUI(player);
            }, 2L);
            return;
        }

        // Ignore border glass panes and info book
        if (type == Material.GRAY_STAINED_GLASS_PANE ||
            type == Material.ORANGE_STAINED_GLASS_PANE ||
            type == Material.BOOK) {
            return;
        }

        // Check if it's the selected kit (green wool) - already selected
        if (type == KIT_SELECTED) {
            player.sendMessage(MessageUtils.format(ORANGE + "kit already selected"));
            return;
        }

        // Check if it's an unselected kit (grey wool)
        if (type != KIT_UNSELECTED) {
            return;
        }

        // Get kit name from clicked item (remove formatting)
        String kitName = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        // Remove the check mark characters if they were stripped but left weirdness
        kitName = kitName.replace("\u2713", "").trim();

        debug(player.getName() + " selected kit: " + kitName);

        // Set selected kit
        kitManager.setSelectedKit(kitName);

        player.sendMessage(MessageUtils.format(ORANGE + "selected kit " + YELLOW + kitName));

        // Refresh GUI
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> openGUI(player), 2L);
    }
}