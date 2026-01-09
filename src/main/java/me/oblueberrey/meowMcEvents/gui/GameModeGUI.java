package me.oblueberrey.meowMcEvents.gui;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameModeGUI implements Listener {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;
    public static final String GUI_TITLE = ChatColor.DARK_AQUA + "Select Game Mode";
    private static final AtomicBoolean listenerRegistered = new AtomicBoolean(false);

    public GameModeGUI(MeowMCEvents plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        if (listenerRegistered.compareAndSet(false, true)) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
        }
    }

    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);

        int currentMode = eventManager.getTeamSize();

        // Solo/FFA Mode (Slot 10)
        gui.setItem(10, createModeItem(
                Material.IRON_SWORD,
                ChatColor.RED + "" + ChatColor.BOLD + "SOLO (FFA)",
                "Free-for-all battle royale",
                "Every player for themselves!",
                "Last player standing wins.",
                1,
                currentMode == 1
        ));

        // 2v2 Mode (Slot 12)
        gui.setItem(12, createModeItem(
                Material.LEATHER_CHESTPLATE,
                ChatColor.GOLD + "" + ChatColor.BOLD + "2v2 TEAMS",
                "Duo team battles",
                "Fight alongside a partner!",
                "Last team standing wins.",
                2,
                currentMode == 2
        ));

        // 3v3 Mode (Slot 14)
        gui.setItem(14, createModeItem(
                Material.IRON_CHESTPLATE,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "3v3 TEAMS",
                "Trio team battles",
                "Squad up with 2 allies!",
                "Last team standing wins.",
                3,
                currentMode == 3
        ));

        // 4v4 Mode (Slot 16)
        gui.setItem(16, createModeItem(
                Material.DIAMOND_CHESTPLATE,
                ChatColor.AQUA + "" + ChatColor.BOLD + "4v4 TEAMS",
                "Quad team battles",
                "Form a 4-player squad!",
                "Last team standing wins.",
                4,
                currentMode == 4
        ));

        // Back Button (Slot 22)
        gui.setItem(22, createBackButton());

        // Fill empty slots
        fillEmptySlots(gui);

        player.openInventory(gui);
    }

    private ItemStack createModeItem(Material material, String name, String desc1, String desc2, String desc3, int teamSize, boolean selected) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + desc1);
        lore.add(ChatColor.GRAY + desc2);
        lore.add(ChatColor.GRAY + desc3);
        lore.add("");

        if (teamSize == 1) {
            lore.add(ChatColor.DARK_GRAY + "Mode: " + ChatColor.WHITE + "Solo (1v1v1...)");
        } else {
            lore.add(ChatColor.DARK_GRAY + "Team Size: " + ChatColor.WHITE + teamSize + " players");
            lore.add(ChatColor.DARK_GRAY + "Format: " + ChatColor.WHITE + teamSize + "v" + teamSize);
        }

        lore.add("");

        if (selected) {
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "CURRENTLY SELECTED");
        } else {
            lore.add(ChatColor.YELLOW + "Click to select this mode");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "BACK");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Return to main menu");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void fillEmptySlots(Inventory gui) {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }

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
        if (!title.equals(GUI_TITLE)) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        switch (slot) {
            case 10: // Solo/FFA
                selectMode(player, 1, "Solo (FFA)");
                break;
            case 12: // 2v2
                selectMode(player, 2, "2v2 Teams");
                break;
            case 14: // 3v3
                selectMode(player, 3, "3v3 Teams");
                break;
            case 16: // 4v4
                selectMode(player, 4, "4v4 Teams");
                break;
            case 22: // Back
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
        player.sendMessage(ChatColor.GREEN + "Game mode set to: " + ChatColor.GOLD + modeName);

        // Refresh GUI to show selection
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> openGUI(player), 1L);
    }
}
