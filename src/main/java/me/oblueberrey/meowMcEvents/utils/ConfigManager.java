package me.oblueberrey.meowMcEvents.utils;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {

    private final MeowMCEvents plugin;
    private FileConfiguration config;

    public ConfigManager(MeowMCEvents plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        validateConfig();
    }

    /**
     * Validate all config values and warn about issues
     */
    private void validateConfig() {
        int issues = 0;

        // Validate border settings
        int borderStart = config.getInt("border.start-size", 300);
        int borderMin = config.getInt("border.shrink-to", 50);
        int borderInterval = config.getInt("border.interval-seconds", 30);

        if (borderStart < 10) {
            plugin.getLogger().warning("[Config] border.start-size is too small (" + borderStart + "). Minimum is 10.");
            config.set("border.start-size", 10);
            issues++;
        }
        if (borderMin < 5) {
            plugin.getLogger().warning("[Config] border.shrink-to is too small (" + borderMin + "). Minimum is 5.");
            config.set("border.shrink-to", 5);
            issues++;
        }
        if (borderMin >= borderStart) {
            plugin.getLogger().warning("[Config] border.shrink-to (" + borderMin + ") must be less than border.start-size (" + borderStart + ").");
            config.set("border.shrink-to", borderStart / 2);
            issues++;
        }
        if (borderInterval < 10) {
            plugin.getLogger().warning("[Config] border.interval-seconds is too small (" + borderInterval + "). Minimum is 10.");
            config.set("border.interval-seconds", 10);
            issues++;
        }

        // Validate event settings (check both new and old paths for backwards compatibility)
        int minPlayers = getMinPlayers();
        int countdown = getCountdownSeconds();
        int defaultMode = getDefaultMode();

        if (minPlayers < 2) {
            plugin.getLogger().warning("[Config] event.min-players must be at least 2. Setting to 2.");
            config.set("event.min-players", 2);
            issues++;
        }
        if (countdown < 10) {
            plugin.getLogger().warning("[Config] event.countdown-seconds is too short (" + countdown + "). Minimum is 10.");
            config.set("event.countdown-seconds", 10);
            issues++;
        }
        if (defaultMode < 1 || defaultMode > 5) {
            plugin.getLogger().warning("[Config] event.default-mode must be 1-5. Setting to 1 (Solo).");
            config.set("event.default-mode", 1);
            issues++;
        }

        // Validate end-spawn-delay-ticks
        int endDelay = getEndSpawnDelayTicks();
        if (endDelay < 0) {
            plugin.getLogger().warning("[Config] event.end-spawn-delay-ticks cannot be negative. Setting to 10.");
            config.set("event.end-spawn-delay-ticks", 10);
            issues++;
        }

        // Validate kits list
        java.util.List<String> kits = config.getStringList("kits");
        if (kits.isEmpty()) {
            plugin.getLogger().warning("[Config] No kits defined! Add kit names to the 'kits' list.");
            issues++;
        }

        String selectedKit = config.getString("selected-kit", "");
        if (!kits.isEmpty() && !kits.contains(selectedKit)) {
            plugin.getLogger().warning("[Config] selected-kit '" + selectedKit + "' is not in the kits list. Using first kit.");
            config.set("selected-kit", kits.get(0));
            issues++;
        }

        // Validate spawn locations exist
        String spawnWorld = config.getString("spawn.world", "world");
        String playerSpawnWorld = config.getString("player-spawn.world", "world");
        String eventSpawnWorld = config.getString("event-spawn.world", "world");

        if (plugin.getServer().getWorld(spawnWorld) == null) {
            plugin.getLogger().warning("[Config] spawn.world '" + spawnWorld + "' does not exist!");
            issues++;
        }
        if (plugin.getServer().getWorld(playerSpawnWorld) == null) {
            plugin.getLogger().warning("[Config] player-spawn.world '" + playerSpawnWorld + "' does not exist!");
            issues++;
        }
        if (plugin.getServer().getWorld(eventSpawnWorld) == null) {
            plugin.getLogger().warning("[Config] event-spawn.world '" + eventSpawnWorld + "' does not exist!");
            issues++;
        }

        // Save any corrections
        if (issues > 0) {
            plugin.saveConfig();
            plugin.getLogger().info("[Config] Found " + issues + " config issue(s). Some values were auto-corrected.");
        } else {
            plugin.getLogger().info("[Config] Configuration validated successfully!");
        }
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
        String message = config.getString("messages." + key, "&cMessage not found: " + key);
        return MessageUtils.colorize(message);
    }

    /**
     * Translate hex color codes (&#RRGGBB) and standard color codes to chat colors
     */
    public static String colorize(String message) {
        return MessageUtils.colorize(message);
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

    // ==================== Event Settings ====================

    public int getDefaultMode() {
        // Check new path first, fall back to old path for backwards compatibility
        if (config.contains("event.default-mode")) {
            return config.getInt("event.default-mode", 1);
        }
        return config.getInt("advanced.default-mode", 1);
    }

    public int getMinPlayers() {
        if (config.contains("event.min-players")) {
            return config.getInt("event.min-players", 2);
        }
        return config.getInt("advanced.min-players", 2);
    }

    public int getCountdownSeconds() {
        if (config.contains("event.countdown-seconds")) {
            return config.getInt("event.countdown-seconds", 60);
        }
        return config.getInt("advanced.countdown-seconds", 60);
    }

    public int getMaxPlayers() {
        return config.getInt("event.max-players", 0);
    }

    public int getEndSpawnDelayTicks() {
        return config.getInt("event.end-spawn-delay-ticks", 10);
    }

    public String getEndCommand() {
        return config.getString("event.end-command", "spawn");
    }

    public boolean isBroadcastToServer() {
        return config.getBoolean("event.broadcast-to-server", true);
    }

    public boolean isAllowMidJoinSpectate() {
        return config.getBoolean("event.allow-mid-join-spectate", true);
    }

    // ==================== Countdown Settings ====================

    public java.util.List<Integer> getCountdownAnnounceTimes() {
        return config.getIntegerList("countdown.announce-times");
    }

    public boolean isCountdownSoundEnabled() {
        return config.getBoolean("countdown.sound-enabled", true);
    }

    public boolean isCountdownTitleEnabled() {
        return config.getBoolean("countdown.title-enabled", true);
    }

    // ==================== Border Settings ====================

    public boolean isBorderEnabled() {
        return config.getBoolean("border.enabled", true);
    }

    public int getBorderShrinkAmount() {
        return config.getInt("border.shrink-amount", 5);
    }

    public double getBorderDamagePerSecond() {
        return config.getDouble("border.damage-per-second", 1.0);
    }

    public int getBorderWarningTime() {
        return config.getInt("border.warning-time", 5);
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

    // ==================== Spectator Settings ====================

    public int getSpectatorDeathDelayTicks() {
        return config.getInt("spectator.death-delay-ticks", 0);
    }

    public boolean canSpectatorsSeEachOther() {
        return config.getBoolean("spectator.see-other-spectators", true);
    }

    public int getCompassSlot() {
        return Math.max(0, Math.min(8, config.getInt("spectator.compass-slot", 4)));
    }

    public int getLeaveSlot() {
        return Math.max(0, Math.min(8, config.getInt("spectator.leave-slot", 8)));
    }

    // ==================== Sound Settings ====================

    public boolean areSoundsEnabled() {
        return config.getBoolean("sounds.enabled", true);
    }

    public float getSoundVolume() {
        return (float) Math.max(0.0, Math.min(1.0, config.getDouble("sounds.volume", 1.0)));
    }

    // ==================== Feedback Settings ====================

    public boolean isBossBarEnabled() {
        return config.getBoolean("feedback.boss-bar-enabled", true);
    }

    public boolean isActionBarEnabled() {
        return config.getBoolean("feedback.action-bar-enabled", true);
    }

    public boolean areParticlesEnabled() {
        return config.getBoolean("feedback.particles-enabled", true);
    }

    public boolean isCelebrationEnabled() {
        return config.getBoolean("feedback.celebration-enabled", true);
    }

    // ==================== PvP Settings ====================

    public int getPvpGracePeriodSeconds() {
        return config.getInt("pvp.grace-period-seconds", 0);
    }

    public boolean isFriendlyFireAllowed() {
        return config.getBoolean("pvp.friendly-fire", false);
    }

    // ==================== Game Settings ====================

    public boolean isDisableFallDamage() {
        return config.getBoolean("game.disable-fall-damage", false);
    }

    public boolean isKeepInventory() {
        return config.getBoolean("game.keep-inventory", true);
    }

    public boolean isAutoBalanceTeams() {
        if (config.contains("teams.auto-balance")) {
            return config.getBoolean("teams.auto-balance", true);
        }
        return config.getBoolean("game.auto-balance-teams", true);
    }

    public boolean isDisableHunger() {
        return config.getBoolean("game.disable-hunger", false);
    }

    public boolean isDisableFireDamage() {
        return config.getBoolean("game.disable-fire-damage", false);
    }

    public boolean isDisableDrowning() {
        return config.getBoolean("game.disable-drowning", false);
    }

    public boolean isDisableExplosionDamage() {
        return config.getBoolean("game.disable-explosion-damage", false);
    }

    public boolean isClearDropsOnDeath() {
        return config.getBoolean("game.clear-drops-on-death", true);
    }

    public boolean isClearExpOnDeath() {
        return config.getBoolean("game.clear-exp-on-death", true);
    }

    // ==================== Team Settings ====================

    public int getBalanceThreshold() {
        return config.getInt("teams.balance-threshold", 2);
    }

    public boolean isShowTeamNametags() {
        return config.getBoolean("teams.show-team-nametags", true);
    }

    public java.util.List<String> getTeamColors() {
        return config.getStringList("teams.colors");
    }

    // ==================== Spectator Extra Settings ====================

    public float getSpectatorFlightSpeed() {
        return (float) Math.max(0.1, Math.min(1.0, config.getDouble("spectator.flight-speed", 0.2)));
    }

    public boolean isCompassTeleportEnabled() {
        return config.getBoolean("spectator.compass-teleport", true);
    }

    public String getSpectatorGamemode() {
        return config.getString("spectator.gamemode", "ADVENTURE");
    }

    public int getSpectatorGracePeriodTicks() {
        return config.getInt("spectator.grace-period-ticks", 60);
    }

    // ==================== PvP Extra Settings ====================

    public boolean isSelfDamageAllowed() {
        return config.getBoolean("pvp.self-damage", true);
    }

    public int getCombatTagSeconds() {
        return config.getInt("pvp.combat-tag-seconds", 0);
    }

    // ==================== Results Extra Settings ====================

    public int getRankingDelayTicks() {
        return config.getInt("results.ranking-delay-ticks", 60);
    }

    public int getCleanupDelayTicks() {
        return config.getInt("results.cleanup-delay-ticks", 160);
    }

    // ==================== Arena Settings ====================

    public int getArenaBoundaryCheckInterval() {
        return Math.max(1, config.getInt("arena.boundary-check-interval", 10));
    }

    public int getArenaDamageZoneSize() {
        return Math.max(0, config.getInt("arena.damage-zone-size", 5));
    }

    public double getArenaDamageZoneMaxDamage() {
        return Math.max(0.5, config.getDouble("arena.damage-zone-max-damage", 4.0));
    }

    // ==================== Results Settings ====================

    public int getMaxPlacements() {
        return config.getInt("results.max-placements", 5);
    }

    // ==================== Command Settings ====================

    public java.util.List<String> getWhitelistedCommands() {
        return config.getStringList("whitelisted-commands");
    }

    public java.util.List<String> getBlacklistedCommands() {
        return config.getStringList("blacklisted-commands");
    }

    public boolean isShowMostKills() {
        return config.getBoolean("results.show-most-kills", true);
    }

    public boolean isShowTotalParticipants() {
        return config.getBoolean("results.show-total-participants", true);
    }
}