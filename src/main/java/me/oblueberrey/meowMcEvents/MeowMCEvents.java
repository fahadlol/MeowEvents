package me.oblueberrey.meowMcEvents;

import me.oblueberrey.meowMcEvents.commands.*;
import me.oblueberrey.meowMcEvents.listeners.*;
import me.oblueberrey.meowMcEvents.managers.*;
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

    @Override
    public void onEnable() {
        instance = this;

        // Initialize config
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Initialize managers
        teamManager = new TeamManager();
        rankManager = new RankManager();
        borderManager = new BorderManager(this);
        kitManager = new KitManager(this);
        eventManager = new EventManager(this, teamManager, rankManager, borderManager, kitManager);

        // Register commands
        getCommand("meowevent").setExecutor(new MeowEventCommand(this, eventManager));
        getCommand("event").setExecutor(new EventJoinCommand(this, eventManager));
        getCommand("leave").setExecutor(new LeaveCommand(this, eventManager));
        getCommand("kits").setExecutor(new KitsCommand(this, eventManager));

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this, eventManager, rankManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(eventManager, teamManager), this);
        getServer().getPluginManager().registerEvents(new BlockListener(eventManager), this);
        getServer().getPluginManager().registerEvents(new CommandBlockListener(this, eventManager), this);
        getServer().getPluginManager().registerEvents(new RegenListener(eventManager), this);

        getLogger().info("MeowMCEvents v1.0 has been enabled!");
        getLogger().info("Loaded " + kitManager.getKitNames().size() + " kits from config");

        if (configManager.isDebugEnabled()) {
            getLogger().info("[DEBUG] Debug mode is ENABLED - detailed logging will be shown in console");
        }
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
}