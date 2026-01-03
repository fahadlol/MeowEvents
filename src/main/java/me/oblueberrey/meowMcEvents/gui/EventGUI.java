package me.oblueberrey.meowMcEvents.gui;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
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

public class EventGUI implements Listener {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;
    private static final String GUI_TITLE = ChatColor.DARK_PURPLE + "MeowMC Event Manager";
    private static boolean listenerRegistered = false;

    public EventGUI(MeowMCEvents plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        // Only register listener once to prevent memory leak
        if (!listenerRegistered) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            listenerRegistered = true;
            debug("EventGUI listener registered");
        }
    }

    private void debug(String message) {
        if (plugin.getConfigManager().shouldLogGui()) {
            plugin.getLogger().info("[DEBUG:GUI] " + message);
        }
    }

    public void openGUI(Player player) {
        debug(player.getName() + " opened Event Manager GUI");
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);

        // Start Event Button (Slot 10)
        gui.setItem(10, createStartButton());

        // Kill Player Button (Slot 12)
        gui.setItem(12, createKillButton());

        // Stop Event Button (Slot 14)
        gui.setItem(14, createStopButton());

        // Kit Selection Button (Slot 11) - NEW!
        gui.setItem(11, createKitButton());

        // Team Size Selector (Slot 16)
        gui.setItem(16, createTeamSizeButton());

        // Toggle Build (Slot 19)
        gui.setItem(19, createBuildToggle());

        // Toggle Natural Regen (Slot 20)
        gui.setItem(20, createNaturalRegenToggle());

        // Toggle Break (Slot 21)
        gui.setItem(21, createBreakToggle());

        // Fill empty slots with glass panes
        fillEmptySlots(gui);

        player.openInventory(gui);
    }

    private ItemStack createStartButton() {
        ItemStack item = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "START EVENT");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Click to start countdown");
        lore.add(ChatColor.GRAY + "Players can join during countdown");
        lore.add(ChatColor.GRAY + "Countdown: " + ChatColor.AQUA + plugin.getConfigManager().getCountdownSeconds() + "s");
        lore.add("");

        if (eventManager.isEventRunning()) {
            lore.add(ChatColor.RED + "⚠ Event already running!");
        } else if (eventManager.isCountdownActive()) {
            lore.add(ChatColor.YELLOW + "⚠ Countdown in progress!");
        } else {
            lore.add(ChatColor.YELLOW + "➤ Click to start");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createKillButton() {
        ItemStack item = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "KILL PLAYER");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Instantly eliminate a player");
        lore.add(ChatColor.GRAY + "Opens player selector");
        lore.add("");
        lore.add(ChatColor.YELLOW + "➤ Click to select player");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStopButton() {
        ItemStack item = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "STOP EVENT");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Stop the current event/countdown");
        lore.add(ChatColor.GRAY + "Resets border and teams");
        lore.add("");

        if (eventManager.isEventRunning()) {
            lore.add(ChatColor.YELLOW + "➤ Click to stop event");
        } else if (eventManager.isCountdownActive()) {
            lore.add(ChatColor.YELLOW + "➤ Click to cancel countdown");
        } else {
            lore.add(ChatColor.RED + "⚠ No event running");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createKitButton() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "SELECT KIT");

        String selectedKit = plugin.getKitManager().getSelectedKit();

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Choose which kit players receive");
        lore.add(ChatColor.GRAY + "when the event starts");
        lore.add("");
        lore.add(ChatColor.GRAY + "Current kit: " + ChatColor.GOLD + selectedKit);
        lore.add("");
        lore.add(ChatColor.YELLOW + "➤ Click to select kit");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTeamSizeButton() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "GAME MODE");

        int teamSize = eventManager.getTeamSize();
        List<String> lore = new ArrayList<>();

        if (teamSize == 1) {
            lore.add(ChatColor.GRAY + "Current: " + ChatColor.YELLOW + "Solo (Free-for-all)");
        } else {
            lore.add(ChatColor.GRAY + "Current: " + ChatColor.YELLOW + teamSize + "v" + teamSize);
        }

        lore.add("");
        lore.add(ChatColor.GRAY + "Available modes:");
        lore.add(ChatColor.YELLOW + "• Solo (1v1v1...)");
        lore.add(ChatColor.YELLOW + "• 2v2");
        lore.add(ChatColor.YELLOW + "• 3v3");
        lore.add(ChatColor.YELLOW + "• 4v4");
        lore.add(ChatColor.YELLOW + "• 5v5");
        lore.add("");
        lore.add(ChatColor.YELLOW + "➤ Click to cycle");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBuildToggle() {
        boolean buildEnabled = eventManager.isBuildingAllowed();
        ItemStack item = new ItemStack(buildEnabled ? Material.GRASS_BLOCK : Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "TOGGLE BUILD");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Allow players to place blocks");
        lore.add("");
        lore.add(ChatColor.GRAY + "Status: " + (buildEnabled ?
                ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
        lore.add("");
        lore.add(ChatColor.YELLOW + "➤ Click to toggle");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNaturalRegenToggle() {
        boolean regenEnabled = eventManager.isNaturalRegenAllowed();
        ItemStack item = new ItemStack(regenEnabled ? Material.GOLDEN_APPLE : Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "NATURAL REGEN");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Allow players to regenerate");
        lore.add(ChatColor.GRAY + "health naturally");
        lore.add("");
        lore.add(ChatColor.GRAY + "Status: " + (regenEnabled ?
                ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
        lore.add("");
        lore.add(ChatColor.YELLOW + "➤ Click to toggle");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBreakToggle() {
        boolean breakEnabled = eventManager.isBreakingAllowed();
        ItemStack item = new ItemStack(breakEnabled ? Material.DIAMOND_PICKAXE : Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "TOGGLE BREAK");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Allow players to break blocks");
        lore.add("");
        lore.add(ChatColor.GRAY + "Status: " + (breakEnabled ?
                ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
        lore.add("");
        lore.add(ChatColor.YELLOW + "➤ Click to toggle");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void fillEmptySlots(Inventory gui) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        String title = event.getView().getTitle();
        if (!title.equals(GUI_TITLE) && !title.contains("Select Player")) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Handle main GUI clicks
        if (title.equals(GUI_TITLE)) {
            handleMainGUIClick(player, event.getSlot(), clicked);
        }
        // Handle player selector clicks
        else if (title.contains("Select Player")) {
            handlePlayerSelectorClick(player, clicked);
        }
    }

    private void handleMainGUIClick(Player player, int slot, ItemStack clicked) {
        switch (slot) {
            case 10: // Start Event
                if (!player.hasPermission("meowevent.admin")) {
                    player.sendMessage(ChatColor.RED + "You need admin permission!");
                    return;
                }
                if (eventManager.isEventRunning() || eventManager.isCountdownActive()) {
                    player.sendMessage(ChatColor.RED + "An event or countdown is already in progress!");
                    return;
                }
                debug(player.getName() + " clicked START EVENT - starting countdown");
                player.closeInventory();
                eventManager.startCountdown();
                break;

            case 11: // Kit Selection - NEW!
                if (!player.hasPermission("meowevent.admin")) {
                    player.sendMessage(ChatColor.RED + "You need admin permission!");
                    return;
                }
                debug(player.getName() + " clicked SELECT KIT");
                player.closeInventory();
                // Open kits GUI
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    KitsGUI kitsGUI = new KitsGUI(plugin, eventManager, true); // true = return to main menu
                    kitsGUI.openGUI(player);
                }, 1L);
                break;

            case 12: // Kill Player
                if (!player.hasPermission("meowevent.admin")) {
                    player.sendMessage(ChatColor.RED + "You need admin permission!");
                    return;
                }
                debug(player.getName() + " clicked KILL PLAYER");
                openPlayerSelector(player);
                break;

            case 14: // Stop Event
                if (!player.hasPermission("meowevent.admin")) {
                    player.sendMessage(ChatColor.RED + "You need admin permission!");
                    return;
                }
                if (!eventManager.isEventRunning() && !eventManager.isCountdownActive()) {
                    player.sendMessage(ChatColor.RED + "No event or countdown is running!");
                    return;
                }
                debug(player.getName() + " clicked STOP EVENT");
                player.closeInventory();
                eventManager.stopEvent();
                break;

            case 16: // Team Size
                if (!player.hasPermission("meowevent.admin")) {
                    player.sendMessage(ChatColor.RED + "You need admin permission!");
                    return;
                }
                debug(player.getName() + " clicked GAME MODE");
                cycleTeamSize(player);
                break;

            case 19: // Toggle Build
                if (!player.hasPermission("meowevent.admin")) {
                    player.sendMessage(ChatColor.RED + "You need admin permission!");
                    return;
                }
                eventManager.toggleBuilding();
                debug(player.getName() + " toggled building to " + eventManager.isBuildingAllowed());
                player.sendMessage(ChatColor.YELLOW + "Building is now " +
                        (eventManager.isBuildingAllowed() ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
                updateGUI(player);
                break;

            case 20: // Toggle Natural Regen
                if (!player.hasPermission("meowevent.admin")) {
                    player.sendMessage(ChatColor.RED + "You need admin permission!");
                    return;
                }
                eventManager.toggleNaturalRegen();
                debug(player.getName() + " toggled natural regen to " + eventManager.isNaturalRegenAllowed());
                player.sendMessage(ChatColor.YELLOW + "Natural Regen is now " +
                        (eventManager.isNaturalRegenAllowed() ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
                updateGUI(player);
                break;

            case 21: // Toggle Break
                if (!player.hasPermission("meowevent.admin")) {
                    player.sendMessage(ChatColor.RED + "You need admin permission!");
                    return;
                }
                eventManager.toggleBreaking();
                debug(player.getName() + " toggled breaking to " + eventManager.isBreakingAllowed());
                player.sendMessage(ChatColor.YELLOW + "Breaking is now " +
                        (eventManager.isBreakingAllowed() ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
                updateGUI(player);
                break;
        }
    }

    private void cycleTeamSize(Player player) {
        int currentSize = eventManager.getTeamSize();
        int newSize = currentSize >= 5 ? 1 : currentSize + 1;
        eventManager.setTeamSize(newSize);

        if (newSize == 1) {
            player.sendMessage(ChatColor.GREEN + "Mode set to Solo (Free-for-all)");
        } else {
            player.sendMessage(ChatColor.GREEN + "Team size set to " + newSize + "v" + newSize);
        }

        updateGUI(player);
    }

    private void updateGUI(Player player) {
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> openGUI(player), 1L);
    }

    private void openPlayerSelector(Player player) {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        int size = ((onlinePlayers.size() + 8) / 9) * 9;
        if (size > 54) size = 54;
        if (size < 9) size = 9;

        Inventory selector = Bukkit.createInventory(null, size,
                ChatColor.RED + "Select Player to Kill");

        for (int i = 0; i < onlinePlayers.size() && i < 54; i++) {
            Player target = onlinePlayers.get(i);
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = skull.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + target.getName());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Health: " + ChatColor.RED +
                    String.format("%.1f", target.getHealth()) + "/" + target.getMaxHealth());
            lore.add("");
            lore.add(ChatColor.RED + "➤ Click to eliminate");

            meta.setLore(lore);
            skull.setItemMeta(meta);
            selector.setItem(i, skull);
        }

        player.openInventory(selector);
    }

    private void handlePlayerSelectorClick(Player admin, ItemStack clicked) {
        if (clicked.getType() != Material.PLAYER_HEAD) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String targetName = ChatColor.stripColor(meta.getDisplayName());
        Player target = Bukkit.getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            admin.sendMessage(ChatColor.RED + "Player not found or offline!");
            admin.closeInventory();
            return;
        }

        admin.closeInventory();
        eventManager.killPlayer(target, admin);
    }
}