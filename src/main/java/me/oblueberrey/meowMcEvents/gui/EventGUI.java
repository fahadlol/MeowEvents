package me.oblueberrey.meowMcEvents.gui;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.utils.ButtonBuilder;
import me.oblueberrey.meowMcEvents.utils.ConfigManager;
import me.oblueberrey.meowMcEvents.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventGUI implements Listener {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;

    // Improved RGB Colors
    private static final String GREY = "&#AAAAAA";
    private static final String YELLOW = "&#FFE566";
    private static final String ORANGE = "&#FF9944";
    private static final String RED = "&#FF5555";
    private static final String GREEN = "&#55FF55";
    private static final String GOLD = "&#FFE566";
    private static final String AQUA = "&#55FFFF";
    private static final String PINK = "&#FF7EB3";

    // Unicode Emojis (Dark grey)
    private static final String E_START = "&#666666▶";
    private static final String E_STOP = "&#666666⏹";
    private static final String E_KIT = "&#666666⚔";
    private static final String E_KILL = "&#666666☠";
    private static final String E_MODE = "&#666666👥";
    private static final String E_GEAR = "&#666666⚙";
    private static final String E_INFO = "&#666666ℹ";

    private static final String GUI_TITLE = MessageUtils.colorize("&#666666⚙ " + MessageUtils.gradient("Event Administration", "#FF7EB3", "#FF9944", "#FFE566"));
    private static final AtomicBoolean listenerRegistered = new AtomicBoolean(false);

    public EventGUI(MeowMCEvents plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        if (listenerRegistered.compareAndSet(false, true)) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            debug("EventGUI listener registered");
        }
    }

    private void debug(String message) {
        if (plugin.getConfigManager().shouldLogGui()) {
            plugin.getLogger().info("[DEBUG:GUI] " + message);
        }
    }

    public void openGUI(Player player) {
        debug(player.getName() + " Opened Event Manager GUI");
        Inventory gui = Bukkit.createInventory(null, 45, GUI_TITLE);

        // Decorative Border
        fillBorder(gui);

        // Group 1: Core Controls (Row 2, centered)
        gui.setItem(11, createStartButton());
        gui.setItem(12, createKitButton());
        gui.setItem(13, createTeamSizeButton());
        gui.setItem(14, createKillButton());
        gui.setItem(15, createStopButton());

        // Group 2: Game Rules (Row 4, centered)
        gui.setItem(29, createBuildToggle());
        gui.setItem(31, createNaturalRegenToggle());
        gui.setItem(33, createBreakToggle());

        // Info Item (Center)
        gui.setItem(4, createInfoItem());

        player.openInventory(gui);
    }

    private void fillBorder(Inventory gui) {
        ItemStack border = new ButtonBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .build();
        ItemStack corner = new ButtonBuilder(Material.ORANGE_STAINED_GLASS_PANE)
                .name(" ")
                .build();

        for (int i = 0; i < gui.getSize(); i++) {
            if (i < 9 || i >= 36 || i % 9 == 0 || (i + 1) % 9 == 0) {
                if (i == 0 || i == 8 || i == 36 || i == 44) {
                    gui.setItem(i, corner);
                } else {
                    gui.setItem(i, border);
                }
            }
        }
    }

    private ItemStack createInfoItem() {
        return new ButtonBuilder(Material.BOOK)
                .name(GOLD + "Event status")
                .lore(GREY + "Current state: " + (eventManager.isEventRunning() ? GREEN + "Running" : (eventManager.isCountdownActive() ? ORANGE + "Countdown" : RED + "Idle")),
                      GREY + "Players: " + YELLOW + eventManager.getJoinedPlayerCount(),
                      GREY + "Mode: " + YELLOW + (eventManager.getTeamSize() == 1 ? "Solo" : eventManager.getTeamSize() + "v" + eventManager.getTeamSize()),
                      "",
                      GREY + "Configure your event settings below.")
                .build();
    }

    private ItemStack createStartButton() {
        ButtonBuilder builder = new ButtonBuilder(Material.LIME_WOOL)
                .name(E_START + " " + GREEN + "Start event")
                .lore(GREY + "Begin the event countdown.",
                      GREY + "Players can join during this time.",
                      "",
                      GREY + "Duration: " + YELLOW + plugin.getConfigManager().getCountdownSeconds() + "s",
                      "");

        if (eventManager.isEventRunning()) {
            builder.addLore(RED + "Status: Event is currently running.");
            builder.glow(true);
        } else if (eventManager.isCountdownActive()) {
            builder.addLore(ORANGE + "Status: Countdown is already active.");
            builder.glow(true);
        } else {
            builder.addLore(YELLOW + "Click to initiate countdown.");
        }

        return builder.build();
    }

    private ItemStack createKillButton() {
        return new ButtonBuilder(Material.IRON_SWORD)
                .name(E_KILL + " " + RED + "Eliminate player")
                .lore(GREY + "Instantly remove a player from the event.",
                      GREY + "Opens a selection menu of online players.",
                      "",
                      YELLOW + "Click to browse players.")
                .build();
    }

    private ItemStack createStopButton() {
        ButtonBuilder builder = new ButtonBuilder(Material.RED_WOOL)
                .name(E_STOP + " " + RED + "End event")
                .lore(GREY + "Stop the current event or countdown.",
                      GREY + "Resets all event-related data.",
                      "");

        if (eventManager.isEventRunning() || eventManager.isCountdownActive()) {
            builder.addLore(YELLOW + "Click to stop immediately.");
        } else {
            builder.addLore(RED + "Status: No active event.");
        }

        return builder.build();
    }

    private ItemStack createKitButton() {
        String selectedKit = plugin.getKitManager().getSelectedKit();
        return new ButtonBuilder(Material.CHEST)
                .name(E_KIT + " " + ORANGE + "Kit selection")
                .lore(GREY + "Select the kit given to all participants.",
                      "",
                      GREY + "Current: " + YELLOW + selectedKit,
                      "",
                      YELLOW + "Click to change kit.")
                .build();
    }

    private ItemStack createTeamSizeButton() {
        int teamSize = eventManager.getTeamSize();
        ButtonBuilder builder = new ButtonBuilder(Material.PLAYER_HEAD)
                .name(E_MODE + " " + YELLOW + "Game format");

        if (teamSize == 1) {
            builder.addLore(GREY + "Format: " + GREEN + "Solo (FFA)");
        } else {
            builder.addLore(GREY + "Format: " + GREEN + teamSize + "v" + teamSize + " Teams");
        }

        builder.addLore("")
                .addLore(GREY + "Select a battle format:")
                .addLore(GREY + "- Solo, 2v2, 3v3, or 4v4")
                .addLore("")
                .addLore(YELLOW + "Click to switch format.");

        return builder.build();
    }

    private ItemStack createBuildToggle() {
        boolean enabled = eventManager.isBuildingAllowed();
        return new ButtonBuilder(enabled ? Material.GRASS_BLOCK : Material.BARRIER)
                .name(E_GEAR + " " + ORANGE + "Building")
                .lore(GREY + "Toggle block placement permissions.",
                      "",
                      GREY + "Status: " + (enabled ? GREEN + "Enabled" : RED + "Disabled"),
                      "",
                      YELLOW + "Click to toggle.")
                .build();
    }

    private ItemStack createNaturalRegenToggle() {
        boolean enabled = eventManager.isNaturalRegenAllowed();
        return new ButtonBuilder(enabled ? Material.GOLDEN_APPLE : Material.BARRIER)
                .name(E_GEAR + " " + ORANGE + "Natural regen")
                .lore(GREY + "Toggle automatic health regeneration.",
                      "",
                      GREY + "Status: " + (enabled ? GREEN + "Enabled" : RED + "Disabled"),
                      "",
                      YELLOW + "Click to toggle.")
                .build();
    }

    private ItemStack createBreakToggle() {
        boolean enabled = eventManager.isBreakingAllowed();
        return new ButtonBuilder(enabled ? Material.DIAMOND_PICKAXE : Material.BARRIER)
                .name(E_GEAR + " " + ORANGE + "Block breaking")
                .lore(GREY + "Toggle block destruction permissions.",
                      "",
                      GREY + "Status: " + (enabled ? GREEN + "Enabled" : RED + "Disabled"),
                      "",
                      YELLOW + "Click to toggle.")
                .build();
    }



    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        String title = event.getView().getTitle();
        if (!title.equals(GUI_TITLE) && !title.contains("Browse players")) {
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
        else if (title.contains("Browse players")) {
            handlePlayerSelectorClick(player, clicked);
        }
    }

    private void handleMainGUIClick(Player player, int slot, ItemStack clicked) {
        switch (slot) {
            case 11: // Start Event
                if (!player.hasPermission("meowevent.admin")) {
                    player.sendMessage(MessageUtils.format(RED + "No permission"));
                    return;
                }
                if (eventManager.isEventRunning() || eventManager.isCountdownActive()) {
                    player.sendMessage(MessageUtils.format(RED + "Event already in progress"));
                    return;
                }
                debug(player.getName() + " Clicked START EVENT - starting countdown");
                player.closeInventory();
                eventManager.startCountdown();
                break;

            case 12: // Kit Selection
                if (!player.hasPermission("meowevent.admin")) {
                    player.sendMessage(MessageUtils.format(RED + "No permission"));
                    return;
                }
                debug(player.getName() + " Clicked SELECT KIT");
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    KitsGUI kitsGUI = new KitsGUI(plugin, eventManager, true);
                    kitsGUI.openGUI(player);
                }, 1L);
                break;

            case 13: // Game Mode
                if (!player.hasPermission("meowevent.admin")) {
                    player.sendMessage(MessageUtils.format(RED + "No permission"));
                    return;
                }
                debug(player.getName() + " clicked GAME MODE - opening selection menu");
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    GameModeGUI gameModeGUI = new GameModeGUI(plugin, eventManager);
                    gameModeGUI.openGUI(player);
                }, 1L);
                break;

            case 14: // Kill Player
                if (!player.hasPermission("meowevent.admin")) {
                    player.sendMessage(MessageUtils.format(RED + "No permission"));
                    return;
                }
                debug(player.getName() + " clicked KILL PLAYER");
                openPlayerSelector(player);
                break;

            case 15: // Stop Event
                if (!player.hasPermission("meowevent.admin")) {
                    player.sendMessage(MessageUtils.format(RED + "No permission"));
                    return;
                }
                if (!eventManager.isEventRunning() && !eventManager.isCountdownActive()) {
                    player.sendMessage(MessageUtils.format(RED + "No event running"));
                    return;
                }
                debug(player.getName() + " Clicked STOP EVENT");
                player.closeInventory();
                eventManager.stopEvent();
                break;

            case 29: // Toggle Build
                if (!player.hasPermission("meowevent.admin")) {
                    player.sendMessage(MessageUtils.format(RED + "No permission"));
                    return;
                }
                eventManager.toggleBuilding();
                debug(player.getName() + " toggled building to " + eventManager.isBuildingAllowed());
                player.sendMessage(MessageUtils.format(ORANGE + "Building " + (eventManager.isBuildingAllowed() ? GREEN + "Enabled" : RED + "Disabled")));
                updateGUI(player);
                break;

            case 31: // Toggle Natural Regen
                if (!player.hasPermission("meowevent.admin")) {
                    player.sendMessage(MessageUtils.format(RED + "No permission"));
                    return;
                }
                eventManager.toggleNaturalRegen();
                debug(player.getName() + " toggled natural regen to " + eventManager.isNaturalRegenAllowed());
                player.sendMessage(MessageUtils.format(ORANGE + "Natural Regen " + (eventManager.isNaturalRegenAllowed() ? GREEN + "Enabled" : RED + "Disabled")));
                updateGUI(player);
                break;

            case 33: // Toggle Break
                if (!player.hasPermission("meowevent.admin")) {
                    player.sendMessage(MessageUtils.format(RED + "No permission"));
                    return;
                }
                eventManager.toggleBreaking();
                debug(player.getName() + " toggled breaking to " + eventManager.isBreakingAllowed());
                player.sendMessage(MessageUtils.format(ORANGE + "Breaking " + (eventManager.isBreakingAllowed() ? GREEN + "Enabled" : RED + "Disabled")));
                updateGUI(player);
                break;
        }
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
                MessageUtils.colorize(GREY + "" + RED + "Browse players"));

        for (int i = 0; i < onlinePlayers.size() && i < 54; i++) {
            Player target = onlinePlayers.get(i);
            
            ItemStack skull = new ButtonBuilder(Material.PLAYER_HEAD)
                    .name(YELLOW + target.getName())
                    .lore(GREY + "Health: " + RED + String.format("%.1f", target.getHealth()) + "/" + (int)target.getMaxHealth(),
                          "",
                          GREY + "UUID: " + GREY + target.getUniqueId().toString().substring(0, 8) + "...",
                          RED + "Click to eliminate player.")
                    .build();
            
            // Safely cast to SkullMeta after null check
            if (skull.getItemMeta() instanceof SkullMeta) {
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                meta.setOwningPlayer(target);
                skull.setItemMeta(meta);
            }
            selector.setItem(i, skull);
        }

        player.openInventory(selector);
    }

    private void handlePlayerSelectorClick(Player admin, ItemStack clicked) {
        if (clicked.getType() != Material.PLAYER_HEAD) return;

        if (!(clicked.getItemMeta() instanceof SkullMeta)) return;
        SkullMeta meta = (SkullMeta) clicked.getItemMeta();

        if (meta.getOwningPlayer() == null) {
            admin.sendMessage(MessageUtils.colorize(GREY + "" + RED + "Invalid player"));
            admin.closeInventory();
            return;
        }

        Player target = meta.getOwningPlayer().getPlayer();

        if (target == null || !target.isOnline()) {
            admin.sendMessage(MessageUtils.colorize(GREY + "" + RED + "Player offline"));
            admin.closeInventory();
            return;
        }

        admin.closeInventory();
        eventManager.killPlayer(target, admin);
    }
}