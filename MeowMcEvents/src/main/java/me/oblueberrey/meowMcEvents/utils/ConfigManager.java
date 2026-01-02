package me.oblueberrey.meowMcEvents.utils;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final MeowMCEvents plugin;
    private FileConfiguration config;

    public ConfigManager(MeowMCEvents plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
    }

    public int getBorderStartSize() {
        return config.getInt("border.start-size", 300);
    }

    public int getBorderShrinkTo() {
        return config.getInt("border.shrink-to", 50);
    }

    public int getBorderIntervalSeconds() {
        return config.getInt("border.interval-seconds", 30);
    }

    public boolean isDefaultBuildingAllowed() {
        return config.getBoolean("game.allow-building", false);
    }

    public boolean isDefaultBreakingAllowed() {
        return config.getBoolean("game.allow-breaking", false);
    }

    public String getMessage(String key) {
        return config.getString("messages." + key, "&cMessage not found: " + key);
    }

    public Location getSpawnLocation() {
        String worldName = config.getString("spawn.world", "world");
        double x = config.getDouble("spawn.x", 0.0);
        double y = config.getDouble("spawn.y", 64.0);
        double z = config.getDouble("spawn.z", 0.0);
        float yaw = (float) config.getDouble("spawn.yaw", 0.0);
        float pitch = (float) config.getDouble("spawn.pitch", 0.0);

        return new Location(plugin.getServer().getWorld(worldName), x, y, z, yaw, pitch);
    }

    public void setSpawnLocation(Location location) {
        config.set("spawn.world", location.getWorld().getName());
        config.set("spawn.x", location.getX());
        config.set("spawn.y", location.getY());
        config.set("spawn.z", location.getZ());
        config.set("spawn.yaw", location.getYaw());
        config.set("spawn.pitch", location.getPitch());
        plugin.saveConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public String getDefaultKit() {
        return config.getString("kit.selected", "default");
    }

    public void setDefaultKit(String kitName) {
        config.set("kit.selected", kitName);
        plugin.saveConfig();
    }

    public int getDefaultMode() {
        return config.getInt("advanced.default-mode", 1);
    }

    public int getMinPlayers() {
        return config.getInt("advanced.min-players", 2);
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("advanced.debug", false);
    }
}