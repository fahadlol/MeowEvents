package me.oblueberrey.meowMcEvents.utils;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogManager {

    private final MeowMCEvents plugin;
    private FileConfiguration logsConfig;
    private File logFile;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // Log levels
    public enum LogLevel {
        ERROR, WARN, INFO, DEBUG
    }

    // Log categories
    public enum Category {
        EVENTS, PLAYERS, TEAMS, SPECTATORS, COMMANDS, FORCESTART, ERRORS
    }

    public LogManager(MeowMCEvents plugin) {
        this.plugin = plugin;
        loadConfig();
        setupLogFile();
    }

    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "logs.yml");
        if (!configFile.exists()) {
            plugin.saveResource("logs.yml", false);
        }
        logsConfig = YamlConfiguration.loadConfiguration(configFile);
    }

    private void setupLogFile() {
        if (!isFileLoggingEnabled()) return;

        File logsFolder = new File(plugin.getDataFolder(), "logs");
        if (!logsFolder.exists()) {
            logsFolder.mkdirs();
        }

        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        logFile = new File(logsFolder, "meowevents-" + date + ".log");

        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
        } catch (IOException e) {
            plugin.getLogger().severe("[LogManager] Failed to create log file: " + e.getMessage());
        }
    }

    public boolean isEnabled() {
        return logsConfig.getBoolean("logging.enabled", true);
    }

    public boolean isFileLoggingEnabled() {
        return logsConfig.getBoolean("logging.file-logging", true);
    }

    public boolean isCategoryEnabled(Category category) {
        return logsConfig.getBoolean("logging.categories." + category.name().toLowerCase(), true);
    }

    public boolean shouldShowStacktrace() {
        return logsConfig.getBoolean("errors.show-stacktrace", true);
    }

    public boolean shouldNotifyAdmins() {
        return logsConfig.getBoolean("errors.notify-admins", false);
    }

    // Main logging methods
    public void log(LogLevel level, Category category, String message) {
        if (!isEnabled()) return;
        if (!isCategoryEnabled(category)) return;

        String formattedMessage = formatMessage(level, category, message);

        // Console output
        switch (level) {
            case ERROR:
                plugin.getLogger().severe(formattedMessage);
                break;
            case WARN:
                plugin.getLogger().warning(formattedMessage);
                break;
            case INFO:
                plugin.getLogger().info(formattedMessage);
                break;
            case DEBUG:
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG] " + formattedMessage);
                }
                break;
        }

        // File output
        writeToFile(level, category, message);
    }

    public void error(Category category, String message) {
        log(LogLevel.ERROR, category, message);
    }

    public void error(Category category, String message, Throwable throwable) {
        log(LogLevel.ERROR, category, message);

        if (shouldShowStacktrace()) {
            throwable.printStackTrace();
        }

        // Log stacktrace to file
        if (isFileLoggingEnabled() && logFile != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                writer.println(dateFormat.format(new Date()) + " [STACKTRACE]");
                throwable.printStackTrace(writer);
                writer.println();
            } catch (IOException e) {
                plugin.getLogger().severe("[LogManager] Failed to write stacktrace to log file");
            }
        }

        // Notify admins if enabled
        if (shouldNotifyAdmins()) {
            notifyAdmins("[ERROR] " + category.name() + ": " + message);
        }
    }

    public void warn(Category category, String message) {
        log(LogLevel.WARN, category, message);
    }

    public void info(Category category, String message) {
        log(LogLevel.INFO, category, message);
    }

    public void debug(Category category, String message) {
        log(LogLevel.DEBUG, category, message);
    }

    // Convenience methods for specific categories
    public void logEvent(String message) {
        info(Category.EVENTS, message);
    }

    public void logPlayer(String message) {
        info(Category.PLAYERS, message);
    }

    public void logTeam(String message) {
        info(Category.TEAMS, message);
    }

    public void logSpectator(String message) {
        info(Category.SPECTATORS, message);
    }

    public void logCommand(String message) {
        info(Category.COMMANDS, message);
    }

    public void logForcestart(String message) {
        info(Category.FORCESTART, message);
    }

    public void logError(String message) {
        error(Category.ERRORS, message);
    }

    public void logError(String message, Throwable throwable) {
        error(Category.ERRORS, message, throwable);
    }

    private String formatMessage(LogLevel level, Category category, String message) {
        return "[" + category.name() + "] " + message;
    }

    private void writeToFile(LogLevel level, Category category, String message) {
        if (!isFileLoggingEnabled() || logFile == null) return;

        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            String timestamp = dateFormat.format(new Date());
            writer.println(timestamp + " [" + level.name() + "] [" + category.name() + "] " + message);
        } catch (IOException e) {
            plugin.getLogger().severe("[LogManager] Failed to write to log file: " + e.getMessage());
        }
    }

    private void notifyAdmins(String message) {
        String colored = ConfigManager.colorize("&#FF5555[MeowEvents] " + message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("meowevent.admin")) {
                player.sendMessage(colored);
            }
        }
    }

    public void reload() {
        loadConfig();
        setupLogFile();
        plugin.getLogger().info("[LogManager] Logging configuration reloaded");
    }
}
