package me.oblueberrey.meowMcEvents.managers;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class KitManager {

    private final MeowMCEvents plugin;
    private String selectedKit;
    private boolean xyrisKitsAvailable;

    public KitManager(MeowMCEvents plugin) {
        this.plugin = plugin;
        this.selectedKit = plugin.getConfig().getString("selected-kit", "Warrior");
        this.xyrisKitsAvailable = checkXyrisKitsAvailable();

        if (!xyrisKitsAvailable) {
            plugin.getLogger().warning("[KitManager] XyrisKits plugin not found! Kit distribution will be disabled.");
        } else {
            debug("XyrisKits plugin detected and available");
        }
    }

    /**
     * Check if XyrisKits plugin is available
     */
    private boolean checkXyrisKitsAvailable() {
        Plugin kitsPlugin = Bukkit.getPluginManager().getPlugin("XyrisKits");
        return kitsPlugin != null && kitsPlugin.isEnabled();
    }

    /**
     * Check if kit distribution is available
     */
    public boolean isKitSystemAvailable() {
        return xyrisKitsAvailable;
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
        if (!xyrisKitsAvailable) {
            debug("Cannot give kit - XyrisKits plugin not available");
            player.sendMessage(ChatColor.RED + "Kit system unavailable - XyrisKits plugin not found!");
            return;
        }

        // SECURITY: Sanitize inputs to prevent command injection
        String safeName = sanitizeInput(player.getName());
        String safeKit = sanitizeInput(kitName);

        // Validate inputs aren't empty after sanitization
        if (safeName.isEmpty() || safeKit.isEmpty()) {
            plugin.getLogger().warning("[SECURITY] Blocked potentially malicious kit command for: " + player.getName());
            player.sendMessage(ChatColor.RED + "Invalid kit name!");
            return;
        }

        // Validate kit exists in our config
        if (!kitExists(safeKit)) {
            debug("Kit '" + safeKit + "' not found in config");
            player.sendMessage(ChatColor.RED + "Kit '" + safeKit + "' does not exist!");
            return;
        }

        // Run command using kit-command format from config
        String commandFormat = plugin.getConfig().getString("kit-command", "kits give %player% %kit%");
        String command = commandFormat
                .replace("%player%", safeName)
                .replace("%kit%", safeKit);
        debug("Executing command: /" + command);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        debug("Kit '" + safeKit + "' given to " + player.getName());
    }

    /**
     * Sanitize input to prevent command injection
     * Only allows alphanumeric characters, underscores, and hyphens
     */
    private String sanitizeInput(String input) {
        if (input == null) return "";
        // Remove any characters that could be used for command injection
        // Only allow: a-z, A-Z, 0-9, underscore, hyphen
        return input.replaceAll("[^a-zA-Z0-9_-]", "");
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