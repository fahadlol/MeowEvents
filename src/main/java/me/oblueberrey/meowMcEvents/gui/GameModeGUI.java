package me.oblueberrey.meowMcEvents.gui;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.utils.ButtonBuilder;
import me.oblueberrey.meowMcEvents.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.atomic.AtomicBoolean;

public class GameModeGUI implements Listener {

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

    public static final String GUI_TITLE = MessageUtils.colorize("&#666666\uD83D\uDCDD " + MessageUtils.gradient("Select Game Format", "#FF7EB3", "#FF9944", "#FFE566"));
    private static final AtomicBoolean listenerRegistered = new AtomicBoolean(false);

    public GameModeGUI(MeowMCEvents plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        if (listenerRegistered.compareAndSet(false, true)) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
        }
    }

    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, GUI_TITLE);

        int currentMode = eventManager.getTeamSize();

        // Decorative Border
        fillBorder(gui);

        // Solo/FFA Mode (Slot 19)
        gui.setItem(19, createModeItem(
                Material.IRON_SWORD,
                RED + "Solo battle",
                "Free-for-all combat.",
                "Every player for themselves!",
                "Last one standing wins.",
                1,
                currentMode == 1
        ));

        // 2v2 Mode (Slot 21)
        gui.setItem(21, createModeItem(
                Material.LEATHER_CHESTPLATE,
                ORANGE + "Duo teams",
                "Two-player squad battles.",
                "Fight alongside your partner!",
                "Cooperation is key.",
                2,
                currentMode == 2
        ));

        // 3v3 Mode (Slot 23)
        gui.setItem(23, createModeItem(
                Material.IRON_CHESTPLATE,
                YELLOW + "Trio teams",
                "Three-player squad battles.",
                "Squad up with two allies!",
                "Dominance through teamwork.",
                3,
                currentMode == 3
        ));

        // 4v4 Mode (Slot 25)
        gui.setItem(25, createModeItem(
                Material.DIAMOND_CHESTPLATE,
                GREEN + "Quad teams",
                "Four-player squad battles.",
                "Form the ultimate team!",
                "Strategic squad play.",
                4,
                currentMode == 4
        ));

        // Back Button (Slot 40)
        gui.setItem(40, createBackButton());

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

    private ItemStack createModeItem(Material material, String name, String desc1, String desc2, String desc3, int teamSize, boolean selected) {
        ButtonBuilder builder = new ButtonBuilder(material)
                .name(name)
                .lore(GREY + desc1)
                .addLore(GREY + desc2)
                .addLore(GREY + desc3)
                .addLore("")
                .addLore(GREY + "Format: " + YELLOW + (teamSize == 1 ? "Solo (FFA)" : teamSize + "v" + teamSize))
                .addLore("");

        if (selected) {
            builder.addLore(GREEN + "Currently selected")
                   .glow(true);
        } else {
            builder.addLore(GOLD + "Click to select format.");
        }

        return builder.build();
    }

    private ItemStack createBackButton() {
        return new ButtonBuilder(Material.ARROW)
                .name(RED + "Back")
                .lore(GREY + "Return to main menu")
                .build();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        String title = event.getView().getTitle();
        if (!title.equals(GUI_TITLE)) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        switch (slot) {
            case 19: // Solo/FFA
                selectMode(player, 1, "Solo (FFA)");
                break;
            case 21: // 2v2
                selectMode(player, 2, "2v2 Teams");
                break;
            case 23: // 3v3
                selectMode(player, 3, "3v3 Teams");
                break;
            case 25: // 4v4
                selectMode(player, 4, "4v4 Teams");
                break;
            case 40: // Back
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    EventGUI eventGUI = new EventGUI(plugin, eventManager);
                    eventGUI.openGUI(player);
                }, 1L);
                break;
        }
    }

    private void selectMode(Player player, int teamSize, String modeName) {
        eventManager.setTeamSize(teamSize);

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        player.sendMessage(MessageUtils.format(ORANGE + "game format set to " + YELLOW + modeName));

        // Refresh GUI to show selection
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> openGUI(player), 1L);
    }
}
