package me.oblueberrey.meowMcEvents.commands;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.utils.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Help command for regular players
 * /meowevents help - shows player commands
 * /meowevents admin - shows admin commands (if permission)
 */
public class MeowEventsHelpCommand implements CommandExecutor, TabCompleter {

    private final MeowMCEvents plugin;
    private final me.oblueberrey.meowMcEvents.managers.EventManager eventManager;

    // Improved RGB Colors
    private static final String GREY = "&#AAAAAA";
    private static final String YELLOW = "&#FFE566";
    private static final String ORANGE = "&#FF9944";
    private static final String GREEN = "&#55FF55";
    private static final String AQUA = "&#55FFFF";
    private static final String PINK = "&#FF7EB3";

    public MeowEventsHelpCommand(MeowMCEvents plugin, me.oblueberrey.meowMcEvents.managers.EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("meowevent.help")) {
            sender.sendMessage(ConfigManager.colorize("&7&cno permission"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendPlayerHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("admin")) {
            if (sender.hasPermission("meowevent.admin")) {
                sendAdminHelp(sender);
            } else {
                sender.sendMessage(ConfigManager.colorize("&7&cno permission"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            sendInfo(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("forcestart")) {
            if (sender.hasPermission("meowevent.admin")) {
                handleForceStart(sender);
            } else {
                sender.sendMessage(ConfigManager.colorize("&7&cno permission"));
            }
            return true;
        }

        // Default to help
        sendPlayerHelp(sender);
        return true;
    }

    private void handleForceStart(CommandSender sender) {
        if (eventManager.isEventRunning()) {
            sender.sendMessage(ConfigManager.colorize("&#FF5555event already running"));
            return;
        }

        // Cancel countdown if active, then force start
        if (eventManager.isCountdownActive()) {
            eventManager.cancelCountdown();
        }

        sender.sendMessage(ConfigManager.colorize("&#55FF55force starting event..."));
        eventManager.startEvent();
    }

    private void sendPlayerHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(ConfigManager.colorize(GREY + "" + YELLOW + "meowevents " + GREY + "-" + ORANGE + "player commands"));
        sender.sendMessage("");
        sender.sendMessage(ConfigManager.colorize(YELLOW + "/event " + GREY + "-" + ORANGE + "join an event or spectate"));
        sender.sendMessage(ConfigManager.colorize(YELLOW + "/eventleave " + GREY + "-" + ORANGE + "leave the current event"));
        sender.sendMessage(ConfigManager.colorize(YELLOW + "/meowevents info " + GREY + "-" + ORANGE + "event info and tips"));
        sender.sendMessage("");

        if (sender.hasPermission("meowevent.admin")) {
            sender.sendMessage(ConfigManager.colorize(GREY + "-" + GREEN + "/meowevents admin " + GREY + "for admin commands"));
            sender.sendMessage("");
        }
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(ConfigManager.colorize(GREY + "" + YELLOW + "meowevents " + GREY + "-" + ORANGE + "admin commands"));
        sender.sendMessage("");
        sender.sendMessage(ConfigManager.colorize(YELLOW + "/meowevent " + GREY + "-" + ORANGE + "open event gui"));
        sender.sendMessage(ConfigManager.colorize(YELLOW + "/meowevent start " + GREY + "-" + ORANGE + "start event countdown"));
        sender.sendMessage(ConfigManager.colorize(YELLOW + "/meowevent stop " + GREY + "-" + ORANGE + "stop current event"));
        sender.sendMessage(ConfigManager.colorize(YELLOW + "/meowevent team <1-5> " + GREY + "-" + ORANGE + "set team size"));
        sender.sendMessage(ConfigManager.colorize(YELLOW + "/meowevent setspawn " + GREY + "-" + ORANGE + "set arena spawn"));
        sender.sendMessage(ConfigManager.colorize(YELLOW + "/meowevent setevent " + GREY + "-" + ORANGE + "set join teleport"));
        sender.sendMessage(ConfigManager.colorize(YELLOW + "/meowevent setplayerspawn " + GREY + "-" + ORANGE + "set respawn point"));
        sender.sendMessage(ConfigManager.colorize(YELLOW + "/meowevent border <sec> " + GREY + "-" + ORANGE + "border interval"));
        sender.sendMessage(ConfigManager.colorize(YELLOW + "/meowevent reload " + GREY + "-" + ORANGE + "reload config"));
        sender.sendMessage(ConfigManager.colorize(YELLOW + "/meowevent debug " + GREY + "-" + ORANGE + "toggle debug mode"));
        sender.sendMessage(ConfigManager.colorize(YELLOW + "/kits " + GREY + "-" + ORANGE + "select event kit"));
        sender.sendMessage("");
    }

    private void sendInfo(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(ConfigManager.colorize(GREY + "" + YELLOW + "meowevents " + GREY + "-" + ORANGE + "event info"));
        sender.sendMessage("");
        sender.sendMessage(ConfigManager.colorize(GREY + "-" + ORANGE + "when an event is announced, use " + YELLOW + "/event " + ORANGE + "to join"));
        sender.sendMessage(ConfigManager.colorize(GREY + "-" + ORANGE + "last player or team standing wins"));
        sender.sendMessage(ConfigManager.colorize(GREY + "-" + ORANGE + "if you die, you become a spectator"));
        sender.sendMessage(ConfigManager.colorize(GREY + "-" + ORANGE + "use " + YELLOW + "/eventleave " + ORANGE + "to exit at any time"));
        sender.sendMessage("");
        sender.sendMessage(ConfigManager.colorize(GREY + "" + YELLOW + "spectator mode"));
        sender.sendMessage(ConfigManager.colorize(GREY + "-" + ORANGE + "compass " + GREY + "-" + ORANGE + "track and teleport to players"));
        sender.sendMessage(ConfigManager.colorize(GREY + "-" + ORANGE + "red dye " + GREY + "-" + ORANGE + "click to leave"));
        sender.sendMessage("");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>(Arrays.asList("help", "info"));
            if (sender.hasPermission("meowevent.admin")) {
                subcommands.add("admin");
                subcommands.add("forcestart");
            }

            String input = args[0].toLowerCase();
            for (String sub : subcommands) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        }

        return completions;
    }
}
