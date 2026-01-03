package me.oblueberrey.meowMcEvents.managers;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public class KitManager {

    private final MeowMCEvents plugin;
    private String selectedKit;

    public KitManager(MeowMCEvents plugin) {
        this.plugin = plugin;
        this.selectedKit = plugin.getConfig().getString("selected-kit", "Warrior");
    }

    private void debug(String message) {
        if (plugin.getConfigManager().shouldLogKits()) {
            plugin.getLogger().info("[DEBUG:KIT] " + message);
        }
    }

    /**
     * Give kit to player by running: /kits give {player} {kit}
     */
    public void giveKit(Player player, String kitName) {
        // Run command: /kits give PlayerName KitName
        String command = "kits give " + player.getName() + " " + kitName;
        debug("Executing command: /" + command);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        debug("Kit '" + kitName + "' given to " + player.getName());
    }

    /**
     * Give the selected kit to a player
     */
    public void giveSelectedKit(Player player) {
        debug("Giving selected kit '" + selectedKit + "' to " + player.getName());
        giveKit(player, selectedKit);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfigManager().getMessage("kit-given").replace("%kit%", selectedKit)));
    }

    /**
     * Set the kit to be used for events
     */
    public void setSelectedKit(String kitName) {
        String previousKit = this.selectedKit;
        this.selectedKit = kitName;
        plugin.getConfig().set("selected-kit", kitName);
        plugin.saveConfig();
        debug("Selected kit changed: " + previousKit + " -> " + kitName);
    }

    /**
     * Get the currently selected kit
     */
    public String getSelectedKit() {
        return selectedKit;
    }

    /**
     * Check if a kit exists in config
     */
    public boolean kitExists(String kitName) {
        List<String> kits = getKitNames();
        return kits.contains(kitName);
    }

    /**
     * Get all kit names from config
     */
    public List<String> getKitNames() {
        return plugin.getConfig().getStringList("kits");
    }
}