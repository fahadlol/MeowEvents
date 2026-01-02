package me.oblueberrey.meowMcEvents;

import me.oblueberrey.meowMcEvents.commands.MeowEventCommand;
import me.oblueberrey.meowMcEvents.listeners.BlockListener;
import me.oblueberrey.meowMcEvents.listeners.PlayerDeathListener;
import me.oblueberrey.meowMcEvents.listeners.PlayerQuitListener;
import me.oblueberrey.meowMcEvents.managers.BorderManager;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.managers.KitManager;
import me.oblueberrey.meowMcEvents.managers.RankManager;
import me.oblueberrey.meowMcEvents.managers.TeamManager;
import me.oblueberrey.meowMcEvents.utils.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MeowMCEvents extends JavaPlugin {

    private static MeowMCEvents instance;
    private EventManager eventManager;
    private TeamManager teamManager;
    private RankManager rankManager;
    private BorderManager borderManager;
    private ConfigManager configManager;
    private KitManager kitManager;
    private boolean xyrisKitsEnabled = false;

    @Override
    public void onEnable() {
        instance = this;

        // Check if XyrisKits is loaded
        if (getServer().getPluginManager().getPlugin("XyrisKits") != null) {
            try {
                Class<?> apiClass = Class.forName("dev.darkxx.xyriskits.api.XyrisKitsAPI");
                apiClass.getMethod("initialize").invoke(null);
                xyrisKitsEnabled = true;
                getLogger().info("Successfully hooked into XyrisKits!");
            } catch (Exception e) {
                getLogger().warning("XyrisKits found but failed to initialize API: " + e.getMessage());
                getLogger().warning("Kit features will be disabled.");
            }
        } else {
            getLogger().warning("XyrisKits not found! Kit features will be disabled.");
            getLogger().warning("Install XyrisKits to use kit selection: /meowevent kit <name>");
        }

        //  config
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        //  managers
        teamManager = new TeamManager();
        rankManager = new RankManager();
        borderManager = new BorderManager(this);
        kitManager = new KitManager(this);
        eventManager = new EventManager(this, teamManager, rankManager, borderManager, kitManager);

        // commands
        getCommand("meowevent").setExecutor(new MeowEventCommand(this, eventManager));

        //  listeners
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(eventManager, rankManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(eventManager, teamManager), this);
        getServer().getPluginManager().registerEvents(new BlockListener(eventManager), this);

        getLogger().info("MeowMCEvents v1.0 has been enabled!");
    }

    @Override
    public void onDisable() {
        // Stop any running event
        if (eventManager != null && eventManager.isEventRunning()) {
            eventManager.stopEvent();
        }

        getLogger().info("MeowMCEvents v1.0 has been disabled!");
    }

    public static MeowMCEvents getInstance() {
        return instance;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public RankManager getRankManager() {
        return rankManager;
    }

    public BorderManager getBorderManager() {
        return borderManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public boolean isXyrisKitsEnabled() {
        return xyrisKitsEnabled;
    }
}