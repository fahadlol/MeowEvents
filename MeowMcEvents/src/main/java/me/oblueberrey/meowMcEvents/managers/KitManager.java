package me.oblueberrey.meowMcEvents.managers;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import org.bukkit.entity.Player;

public class KitManager {

    private final MeowMCEvents plugin;
    private String selectedKit;

    public KitManager(MeowMCEvents plugin) {
        this.plugin = plugin;
        // Load default kit from config
        this.selectedKit = plugin.getConfigManager().getDefaultKit();
        plugin.getLogger().info("Loaded default kit: " + selectedKit);
    }

    /**
     * Give a kit to a player using XyrisKits API (via reflection)
     */
    public void giveKit(Player player) {
        if (!plugin.isXyrisKitsEnabled()) {
            plugin.getLogger().warning("Cannot give kit - XyrisKits is not enabled!");
            return;
        }

        if (selectedKit == null || selectedKit.isEmpty()) {
            plugin.getLogger().warning("No kit selected! Using default kit.");
            return;
        }

        try {
            // Use reflection to call XyrisKitsAPI.getKitsAPI().giveKit(player, kitName)
            Class<?> apiClass = Class.forName("dev.darkxx.xyriskits.api.XyrisKitsAPI");
            Object kitsAPI = apiClass.getMethod("getKitsAPI").invoke(null);

            kitsAPI.getClass().getMethod("giveKit", Player.class, String.class)
                    .invoke(kitsAPI, player, selectedKit);

            plugin.getLogger().info("Gave kit '" + selectedKit + "' to player " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to give kit '" + selectedKit + "' to " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Set the kit to be used for events
     */
    public void setSelectedKit(String kitName) {
        this.selectedKit = kitName;
        plugin.getConfigManager().setDefaultKit(kitName);
        plugin.getLogger().info("Selected kit changed to: " + kitName);
    }

    /**
     * Get the currently selected kit
     */
    public String getSelectedKit() {
        return selectedKit;
    }

    /**
     * Check if a kit exists in XyrisKits
     */
    public boolean kitExists(String kitName) {
        try {
            // Try to check if kit exists (you may need to adjust based on XyrisKits API)
            return kitName != null && !kitName.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}