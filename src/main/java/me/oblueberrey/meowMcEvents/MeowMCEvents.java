package me.oblueberrey.meowMcEvents;

import me.oblueberrey.meowMcEvents.commands.*;
import me.oblueberrey.meowMcEvents.listeners.*;
import me.oblueberrey.meowMcEvents.managers.*;
import me.oblueberrey.meowMcEvents.utils.ConfigManager;
import me.oblueberrey.meowMcEvents.utils.EventFeedback;
import me.oblueberrey.meowMcEvents.utils.LicenseManager;
import me.oblueberrey.meowMcEvents.utils.LogManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class MeowMCEvents extends JavaPlugin {

    private static MeowMCEvents instance;
    private EventManager eventManager;
    private TeamManager teamManager;
    private KillStreakManager killStreakManager;
    private BorderManager borderManager;
    private ConfigManager configManager;
    private KitManager kitManager;
    private SpectatorCompassListener spectatorCompassListener;
    private EventStatsManager eventStatsManager;
    private EventFeedback eventFeedback;
    private LicenseManager licenseManager;
    private ScoreboardManager scoreboardManager;
    private TabListManager tabListManager;
    private KillFeedManager killFeedManager;
    private ArenaManager arenaManager;
    private ArenaBoundaryListener arenaBoundaryListener;
    private LogManager logManager;
    private DamageTracker damageTracker;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize config
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Initialize logging
        logManager = new LogManager(this);
        logManager.info(LogManager.Category.EVENTS, "MeowMCEvents starting up...");

        // License check
        licenseManager = new LicenseManager(this);
        licenseManager.validate();

        // Initialize managers
        teamManager = new TeamManager();
        killStreakManager = new KillStreakManager();
        borderManager = new BorderManager(this);
        kitManager = new KitManager(this);
        eventStatsManager = new EventStatsManager(this);
        eventFeedback = new EventFeedback(this);
        scoreboardManager = new ScoreboardManager(this);
        tabListManager = new TabListManager(this);
        killFeedManager = new KillFeedManager(this);
        damageTracker = new DamageTracker(this);
        eventManager = new EventManager(this, teamManager, killStreakManager, borderManager, kitManager, eventStatsManager, eventFeedback);

        // Initialize arena system
        arenaManager = new ArenaManager(this);
        arenaManager.loadArenas();
        arenaBoundaryListener = new ArenaBoundaryListener(this, eventManager, arenaManager);
        eventManager.setArenaManager(arenaManager);
        eventManager.setArenaBoundaryListener(arenaBoundaryListener);

        // Register commands
        getCommand("meowevent").setExecutor(new MeowEventCommand(this, eventManager));
        getCommand("meowevents").setExecutor(new MeowEventsHelpCommand(this, eventManager));
        getCommand("event").setExecutor(new EventJoinCommand(this, eventManager));
        getCommand("eventleave").setExecutor(new LeaveCommand(this, eventManager));
        getCommand("kits").setExecutor(new KitsCommand(this, eventManager));

        // Register new commands
        PluginCommand arenaCmd = getCommand("arena");
        if (arenaCmd != null) {
            ArenaCommand arenaCommand = new ArenaCommand(this, arenaManager);
            arenaCmd.setExecutor(arenaCommand);
            arenaCmd.setTabCompleter(arenaCommand);
        }
        PluginCommand spectateCmd = getCommand("eventspectate");
        if (spectateCmd != null) {
            spectateCmd.setExecutor(new EventSpectateCommand(this, eventManager));
        }

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this, eventManager, killStreakManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(eventManager, teamManager), this);
        getServer().getPluginManager().registerEvents(new BlockListener(eventManager), this);
        getServer().getPluginManager().registerEvents(new CommandBlockListener(this, eventManager), this);
        getServer().getPluginManager().registerEvents(new RegenListener(eventManager), this);
        getServer().getPluginManager().registerEvents(new WaitingAreaListener(this, eventManager), this);
        getServer().getPluginManager().registerEvents(new PvPListener(this, eventManager), this);
        getServer().getPluginManager().registerEvents(new PlayerRespawnListener(this, eventManager), this);
        getServer().getPluginManager().registerEvents(new DamageListener(this, eventManager), this);

        // Spectator compass listener
        spectatorCompassListener = new SpectatorCompassListener(this, eventManager, teamManager);
        getServer().getPluginManager().registerEvents(spectatorCompassListener, this);

        // Spectator protection listener
        getServer().getPluginManager().registerEvents(new SpectatorProtectionListener(this, eventManager), this);

        // Fatal damage listener - intercepts death and converts to spectator without dying
        getServer().getPluginManager().registerEvents(new FatalDamageListener(this, eventManager, killStreakManager), this);

        // Void and command listener - handles void deaths and /kill command interception
        getServer().getPluginManager().registerEvents(new VoidAndCommandListener(this, eventManager), this);

        // Damage attribution listener - tracks projectiles, explosions, fire/lava for kill credit
        getServer().getPluginManager().registerEvents(new DamageAttributionListener(this, eventManager, damageTracker), this);

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

        // Stop arena boundary checking
        if (arenaBoundaryListener != null) {
            arenaBoundaryListener.stopBoundaryCheck();
        }

        // Save arenas
        if (arenaManager != null) {
            arenaManager.saveArenas();
        }

        // Clean up event feedback (remove boss bar, stop tasks)
        if (eventFeedback != null) {
            eventFeedback.removeBossBar();
        }

        // Clean up scoreboard
        if (scoreboardManager != null) {
            scoreboardManager.stopScoreboard();
        }

        // Clean up tab list
        if (tabListManager != null) {
            tabListManager.stopTabList();
        }

        // Shutdown damage tracker
        if (damageTracker != null) {
            damageTracker.shutdown();
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

    public KillStreakManager getKillStreakManager() {
        return killStreakManager;
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

    public SpectatorCompassListener getSpectatorCompassListener() {
        return spectatorCompassListener;
    }

    public EventStatsManager getEventStatsManager() {
        return eventStatsManager;
    }

    public EventFeedback getEventFeedback() {
        return eventFeedback;
    }

    public LicenseManager getLicenseManager() {
        return licenseManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public TabListManager getTabListManager() {
        return tabListManager;
    }


    public KillFeedManager getKillFeedManager() {
        return killFeedManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public ArenaBoundaryListener getArenaBoundaryListener() {
        return arenaBoundaryListener;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public DamageTracker getDamageTracker() {
        return damageTracker;
    }
}