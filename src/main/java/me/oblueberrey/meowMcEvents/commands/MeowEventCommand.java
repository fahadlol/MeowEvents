package me.oblueberrey.meowMcEvents.commands;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.gui.EventGUI;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.utils.ConfigManager;
import me.oblueberrey.meowMcEvents.utils.LogManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MeowEventCommand implements CommandExecutor, TabCompleter {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;

    // Improved RGB Colors
    private static final String GREY = "&#AAAAAA";
    private static final String YELLOW = "&#FFE566";
    private static final String ORANGE = "&#FF9944";
    private static final String RED = "&#FF5555";
    private static final String GREEN = "&#55FF55";
    private static final String AQUA = "&#55FFFF";
    private static final String PINK = "&#FF7EB3";

    // Small caps alphabet for stylized text
    private static final String SMALL_CAPS = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";

    public MeowEventCommand(MeowMCEvents plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    private String msg(String text) {
        return ConfigManager.colorize(text);
    }

    private String toSmallCaps(String text) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toLowerCase().toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                result.append(SMALL_CAPS.charAt(c - 'a'));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg(GREY + "" + RED + "players only"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("meowevent.admin")) {
            player.sendMessage(msg("&7&cno permission"));
            return true;
        }

        // No arguments - open GUI
        if (args.length == 0) {
            EventGUI gui = new EventGUI(plugin, eventManager);
            gui.openGUI(player);
            return true;
        }

        // Handle subcommands
        switch (args[0].toLowerCase()) {
            case "start":
                handleStart(player);
                break;

            case "stop":
                handleStop(player);
                break;

            case "setspawn":
            case "spawn":
                handleSetSpawn(player);
                break;

            case "setplayerspawn":
            case "playerspawn":
                handleSetPlayerSpawn(player);
                break;

            case "setevent":
            case "eventspawn":
                handleSetEventSpawn(player);
                break;

            case "team":
                if (args.length < 2) {
                    player.sendMessage(msg(GREY + "-" + ORANGE + "usage " + GREY + "-" + YELLOW + "/meowevent team <size>"));
                    player.sendMessage(msg(GREY + "-" + ORANGE + "sizes " + GREY + "-" + YELLOW + "1=solo, 2-5=teams"));
                    return true;
                }
                handleTeamSize(player, args[1]);
                break;

            case "border":
                if (args.length < 2) {
                    player.sendMessage(msg(GREY + "-" + ORANGE + "usage " + GREY + "-" + YELLOW + "/meowevent border <seconds>"));
                    player.sendMessage(msg(GREY + "-" + ORANGE + "sets border shrink interval"));
                    return true;
                }
                handleBorderInterval(player, args[1]);
                break;

            case "help":
                sendHelpMessage(player);
                break;

            case "reload":
                handleReload(player);
                break;

            case "debug":
                handleDebugToggle(player);
                break;

            case "forcestart":
                handleForceStart(player);
                break;

            default:
                player.sendMessage(msg(GREY + "" + RED + "unknown command " + GREY + "-" + ORANGE + "/meowevent help"));
                break;
        }

        return true;
    }

    private void handleStart(Player player) {
        if (!player.hasPermission("meowevent.admin")) {
            player.sendMessage(msg("&7&cno permission"));
            return;
        }

        if (eventManager.isEventRunning()) {
            player.sendMessage(plugin.getConfigManager().getMessage("already-running"));
            return;
        }

        if (eventManager.isCountdownActive()) {
            player.sendMessage(msg(GREY + "" + RED + "countdown already active"));
            return;
        }

        int countdownSeconds = plugin.getConfigManager().getCountdownSeconds();
        player.sendMessage(msg(GREY + "" + GREEN + "starting " + YELLOW + countdownSeconds + "s " + ORANGE + "countdown"));
        player.sendMessage(msg(GREY + "-" + toSmallCaps("kit") + " " + GREY + "-" + YELLOW + plugin.getKitManager().getSelectedKit()));

        eventManager.startCountdown();
    }

    private void handleForceStart(Player player) {
        LogManager log = plugin.getLogManager();

        if (log != null) log.info(LogManager.Category.FORCESTART, "Forcestart initiated by " + player.getName());

        if (!player.hasPermission("meowevent.admin")) {
            if (log != null) log.warn(LogManager.Category.FORCESTART, "DENIED - " + player.getName() + " lacks meowevent.admin permission");
            player.sendMessage(msg(RED + "no permission"));
            return;
        }

        if (eventManager.isEventRunning()) {
            if (log != null) log.warn(LogManager.Category.FORCESTART, "FAILED - Event already running");
            player.sendMessage(msg(RED + "event already running"));
            return;
        }

        // Check if countdown is active (players can only join during countdown)
        if (!eventManager.isCountdownActive()) {
            if (log != null) log.error(LogManager.Category.FORCESTART, "FAILED - No countdown active. State: " + eventManager.getState());
            player.sendMessage(msg(RED + "no countdown active " + GREY + "- " + YELLOW + "use /meowevent start first"));
            return;
        }

        // Check minimum 2 players required
        int playerCount = eventManager.getJoinedPlayerCount();
        if (log != null) log.info(LogManager.Category.FORCESTART, "Player count check: " + playerCount + " players joined");

        if (playerCount < 2) {
            if (log != null) log.error(LogManager.Category.FORCESTART, "FAILED - Not enough players. Need 2, got " + playerCount);
            player.sendMessage(msg(RED + "need at least 2 players to start " + GREY + "(" + YELLOW + playerCount + GREY + " joined)"));
            return;
        }

        // Stop countdown task without clearing players, then force start
        if (log != null) log.info(LogManager.Category.FORCESTART, "Stopping countdown task (keeping " + playerCount + " players)");
        eventManager.stopCountdownOnly();

        if (log != null) log.info(LogManager.Category.FORCESTART, "SUCCESS - Force starting event with " + playerCount + " players");
        player.sendMessage(msg(GREEN + "force starting event " + GREY + "(" + YELLOW + playerCount + GREY + " players)"));

        try {
            eventManager.startEvent();
            if (log != null) log.info(LogManager.Category.FORCESTART, "startEvent() called successfully");
        } catch (Exception e) {
            if (log != null) log.error(LogManager.Category.ERRORS, "EXCEPTION during forcestart: " + e.getMessage(), e);
            player.sendMessage(msg(RED + "error starting event - check console"));
        }
    }

    private void handleStop(Player player) {
        if (!player.hasPermission("meowevent.admin")) {
            player.sendMessage(msg(GREY + "" + RED + "no permission"));
            return;
        }

        if (!eventManager.isEventRunning() && !eventManager.isCountdownActive()) {
            player.sendMessage(msg(GREY + "" + RED + "no event running"));
            return;
        }

        if (eventManager.isCountdownActive()) {
            player.sendMessage(msg(GREY + "" + ORANGE + "cancelling countdown..."));
        } else {
            player.sendMessage(msg(GREY + "" + ORANGE + "stopping event..."));
        }

        eventManager.stopEvent();
    }

    private void handleSetSpawn(Player player) {
        if (!player.hasPermission("meowevent.admin")) {
            player.sendMessage(msg(GREY + "" + RED + "no permission"));
            return;
        }

        plugin.getConfigManager().setSpawnLocation(player.getLocation());
        player.sendMessage(msg(GREY + "" + GREEN + toSmallCaps("event spawn set")));
        player.sendMessage(msg(GREY + "-" + ORANGE + player.getWorld().getName() + " " + YELLOW +
                (int)player.getLocation().getX() + " " + (int)player.getLocation().getY() + " " + (int)player.getLocation().getZ()));
    }

    private void handleSetPlayerSpawn(Player player) {
        if (!player.hasPermission("meowevent.admin")) {
            player.sendMessage(msg(GREY + "" + RED + "no permission"));
            return;
        }

        plugin.getConfigManager().setPlayerSpawnLocation(player.getLocation());
        player.sendMessage(msg(GREY + "" + GREEN + toSmallCaps("player spawn set")));
        player.sendMessage(msg(GREY + "-" + ORANGE + "tp here on death/leave"));
        player.sendMessage(msg(GREY + "-" + ORANGE + player.getWorld().getName() + " " + YELLOW +
                (int)player.getLocation().getX() + " " + (int)player.getLocation().getY() + " " + (int)player.getLocation().getZ()));
    }

    private void handleSetEventSpawn(Player player) {
        if (!player.hasPermission("meowevent.admin")) {
            player.sendMessage(msg(GREY + "" + RED + "no permission"));
            return;
        }

        plugin.getConfigManager().setEventJoinSpawnLocation(player.getLocation());
        player.sendMessage(msg(GREY + "" + GREEN + toSmallCaps("event join spawn set")));
        player.sendMessage(msg(GREY + "-" + ORANGE + "tp here on /event"));
        player.sendMessage(msg(GREY + "-" + ORANGE + player.getWorld().getName() + " " + YELLOW +
                (int)player.getLocation().getX() + " " + (int)player.getLocation().getY() + " " + (int)player.getLocation().getZ()));
    }

    private void handleTeamSize(Player player, String sizeArg) {
        if (!player.hasPermission("meowevent.admin")) {
            player.sendMessage(msg(GREY + "" + RED + "no permission"));
            return;
        }

        try {
            int size = Integer.parseInt(sizeArg);

            if (size < 1 || size > 5) {
                player.sendMessage(msg(GREY + "" + RED + "size must be 1-5"));
                player.sendMessage(msg(GREY + "-" + ORANGE + "1=solo, 2=2v2, 3=3v3..."));
                return;
            }

            eventManager.setTeamSize(size);

            if (size == 1) {
                player.sendMessage(msg(GREY + "" + GREEN + toSmallCaps("solo mode") + " " + ORANGE + "(free-for-all)"));
            } else {
                player.sendMessage(msg(GREY + "" + GREEN + toSmallCaps("team mode") + " " + YELLOW + size + "v" + size));
            }

        } catch (NumberFormatException e) {
            player.sendMessage(msg(GREY + "" + RED + "invalid number"));
        }
    }

    private void handleBorderInterval(Player player, String intervalArg) {
        if (!player.hasPermission("meowevent.admin")) {
            player.sendMessage(msg(GREY + "" + RED + "no permission"));
            return;
        }

        try {
            int seconds = Integer.parseInt(intervalArg);

            if (seconds < 10) {
                player.sendMessage(msg(GREY + "" + RED + "minimum 10 seconds"));
                return;
            }

            plugin.getBorderManager().setShrinkInterval(seconds);
            player.sendMessage(msg(GREY + "" + GREEN + toSmallCaps("border interval") + " " + YELLOW + seconds + "s"));

        } catch (NumberFormatException e) {
            player.sendMessage(msg(GREY + "" + RED + "invalid number"));
        }
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("meowevent.admin")) {
            player.sendMessage(msg(GREY + "" + RED + "no permission"));
            return;
        }

        plugin.getConfigManager().reload();
        plugin.getArenaManager().loadArenas();

        // Reload logs config
        if (plugin.getLogManager() != null) {
            plugin.getLogManager().reload();
        }

        boolean valid = plugin.getLicenseManager().validate();

        player.sendMessage(msg(GREY + "" + GREEN + toSmallCaps("config reloaded")));

        if (!valid) {
            player.sendMessage(msg(GREY + "" + RED + "invalid license - restricted mode"));
        } else {
            player.sendMessage(msg(GREY + "" + GREEN + "license validated"));
        }

        if (plugin.getConfigManager().isDebugEnabled()) {
            player.sendMessage(msg(GREY + "-" + YELLOW + "debug " + GREEN + "enabled"));
        } else {
            player.sendMessage(msg(GREY + "-" + YELLOW + "debug " + GREY + "disabled"));
        }
    }

    private void handleDebugToggle(Player player) {
        if (!player.hasPermission("meowevent.admin")) {
            player.sendMessage(msg(GREY + "" + RED + "no permission"));
            return;
        }

        boolean currentState = plugin.getConfigManager().isDebugEnabled();
        plugin.getConfig().set("debug.enabled", !currentState);
        plugin.saveConfig();
        plugin.getConfigManager().reload();

        if (!currentState) {
            player.sendMessage(msg(GREY + "" + GREEN + "debug " + YELLOW + "enabled"));
            plugin.getLogger().info("[DEBUG] Debug mode enabled by " + player.getName());
        } else {
            player.sendMessage(msg(GREY + "" + ORANGE + "debug " + GREY + "disabled"));
        }
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage("");
        player.sendMessage(msg(GREY + "" + YELLOW + toSmallCaps("meowevents") + " " + GREY + "-" + ORANGE + toSmallCaps("admin commands")));
        player.sendMessage("");
        player.sendMessage(msg(YELLOW + "/meowevent " + GREY + "-" + ORANGE + "open gui"));
        player.sendMessage(msg(YELLOW + "/meowevent start " + GREY + "-" + ORANGE + "start countdown"));
        player.sendMessage(msg(YELLOW + "/meowevent forcestart " + GREY + "-" + ORANGE + "skip countdown"));
        player.sendMessage(msg(YELLOW + "/meowevent stop " + GREY + "-" + ORANGE + "stop event"));
        player.sendMessage(msg(YELLOW + "/meowevent setspawn " + GREY + "-" + ORANGE + "set arena spawn"));
        player.sendMessage(msg(YELLOW + "/meowevent setevent " + GREY + "-" + ORANGE + "set join tp"));
        player.sendMessage(msg(YELLOW + "/meowevent setplayerspawn " + GREY + "-" + ORANGE + "set respawn"));
        player.sendMessage(msg(YELLOW + "/meowevent team <1-5> " + GREY + "-" + ORANGE + "set mode"));
        player.sendMessage(msg(YELLOW + "/meowevent border <sec> " + GREY + "-" + ORANGE + "border interval"));
        player.sendMessage(msg(YELLOW + "/meowevent reload " + GREY + "-" + ORANGE + "reload config"));
        player.sendMessage(msg(YELLOW + "/meowevent debug " + GREY + "-" + ORANGE + "toggle debug"));
        player.sendMessage(msg(YELLOW + "/kits " + GREY + "-" + ORANGE + "select kit"));
        player.sendMessage("");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("start", "forcestart", "stop", "setspawn", "setplayerspawn", "setevent", "team", "border", "reload", "debug", "help");
            String input = args[0].toLowerCase();

            for (String subcommand : subcommands) {
                if (subcommand.startsWith(input)) {
                    completions.add(subcommand);
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("team")) {
                completions.addAll(Arrays.asList("1", "2", "3", "4", "5"));
            } else if (args[0].equalsIgnoreCase("border")) {
                completions.addAll(Arrays.asList("10", "20", "30", "60"));
            }
        }

        return completions;
    }
}