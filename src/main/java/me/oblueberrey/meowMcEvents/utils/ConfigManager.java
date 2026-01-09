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

    public boolean isDefaultNaturalRegenAllowed() {
        return config.getBoolean("game.allow-natural-regen", true);
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

        org.bukkit.World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Spawn world '" + worldName + "' not found! Using default world.");
            world = plugin.getServer().getWorlds().isEmpty() ? null : plugin.getServer().getWorlds().get(0);
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    public Location getPlayerSpawnLocation() {
        String worldName = config.getString("player-spawn.world", "world");
        double x = config.getDouble("player-spawn.x", 0.0);
        double y = config.getDouble("player-spawn.y", 64.0);
        double z = config.getDouble("player-spawn.z", 0.0);
        float yaw = (float) config.getDouble("player-spawn.yaw", 0.0);
        float pitch = (float) config.getDouble("player-spawn.pitch", 0.0);

        org.bukkit.World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Player spawn world '" + worldName + "' not found! Using default world.");
            world = plugin.getServer().getWorlds().isEmpty() ? null : plugin.getServer().getWorlds().get(0);
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    public Location getEventJoinSpawnLocation() {
        String worldName = config.getString("event-spawn.world", "world");
        double x = config.getDouble("event-spawn.x", 0.0);
        double y = config.getDouble("event-spawn.y", 64.0);
        double z = config.getDouble("event-spawn.z", 0.0);
        float yaw = (float) config.getDouble("event-spawn.yaw", 0.0);
        float pitch = (float) config.getDouble("event-spawn.pitch", 0.0);

        org.bukkit.World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Event spawn world '" + worldName + "' not found! Using default world.");
            world = plugin.getServer().getWorlds().isEmpty() ? null : plugin.getServer().getWorlds().get(0);
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    public void setEventJoinSpawnLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            plugin.getLogger().warning("Cannot set event spawn: location or world is null!");
            return;
        }
        config.set("event-spawn.world", location.getWorld().getName());
        config.set("event-spawn.x", location.getX());
        config.set("event-spawn.y", location.getY());
        config.set("event-spawn.z", location.getZ());
        config.set("event-spawn.yaw", location.getYaw());
        config.set("event-spawn.pitch", location.getPitch());
        plugin.saveConfig();
    }

    public void setSpawnLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            plugin.getLogger().warning("Cannot set spawn: location or world is null!");
            return;
        }
        config.set("spawn.world", location.getWorld().getName());
        config.set("spawn.x", location.getX());
        config.set("spawn.y", location.getY());
        config.set("spawn.z", location.getZ());
        config.set("spawn.yaw", location.getYaw());
        config.set("spawn.pitch", location.getPitch());
        plugin.saveConfig();
    }

    public void setPlayerSpawnLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            plugin.getLogger().warning("Cannot set player spawn: location or world is null!");
            return;
        }
        config.set("player-spawn.world", location.getWorld().getName());
        config.set("player-spawn.x", location.getX());
        config.set("player-spawn.y", location.getY());
        config.set("player-spawn.z", location.getZ());
        config.set("player-spawn.yaw", location.getYaw());
        config.set("player-spawn.pitch", location.getPitch());
        plugin.saveConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public int getDefaultMode() {
        return config.getInt("advanced.default-mode", 1);
    }

    public int getMinPlayers() {
        return config.getInt("advanced.min-players", 2);
    }

    public int getCountdownSeconds() {
        return config.getInt("advanced.countdown-seconds", 60);
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("debug.enabled", false);
    }

    public boolean shouldLogEvents() {
        return isDebugEnabled() && config.getBoolean("debug.log-events", true);
    }

    public boolean shouldLogPlayers() {
        return isDebugEnabled() && config.getBoolean("debug.log-players", true);
    }

    public boolean shouldLogTeams() {
        return isDebugEnabled() && config.getBoolean("debug.log-teams", true);
    }

    public boolean shouldLogBorder() {
        return isDebugEnabled() && config.getBoolean("debug.log-border", true);
    }

    public boolean shouldLogKits() {
        return isDebugEnabled() && config.getBoolean("debug.log-kits", true);
    }

    public boolean shouldLogGui() {
        return isDebugEnabled() && config.getBoolean("debug.log-gui", true);
    }
}