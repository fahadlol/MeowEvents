package me.oblueberrey.meowMcEvents.commands;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.gui.EventGUI;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import org.bukkit.ChatColor;
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

    public MeowEventCommand(MeowMCEvents plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("meowevent.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
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
                    player.sendMessage(ChatColor.RED + "Usage: /meowevent team <size>");
                    player.sendMessage(ChatColor.YELLOW + "Valid sizes: 1 (Solo), 2-5 (Teams)");
                    return true;
                }
                handleTeamSize(player, args[1]);
                break;

            case "border":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /meowevent border <seconds>");
                    player.sendMessage(ChatColor.YELLOW + "Sets border shrink interval in seconds");
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

            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /meowevent help for commands.");
                break;
        }

        return true;
    }

    private void handleStart(Player player) {
        if (!player.hasPermission("meowevent.admin")) {
            player.sendMessage(ChatColor.RED + "You need admin permission to start events!");
            return;
        }

        if (eventManager.isEventRunning()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfigManager().getMessage("already-running")));
            return;
        }

        if (eventManager.isCountdownActive()) {
            player.sendMessage(ChatColor.RED + "A countdown is already in progress!");
            return;
        }

        int countdownSeconds = plugin.getConfigManager().getCountdownSeconds();
        player.sendMessage(ChatColor.YELLOW + "Starting " + countdownSeconds + " second countdown...");
        player.sendMessage(ChatColor.GRAY + "Selected kit: " + ChatColor.GOLD + plugin.getKitManager().getSelectedKit());

        eventManager.startCountdown();
    }

    private void handleStop(Player player) {
        if (!player.hasPermission("meowevent.admin")) {
            player.sendMessage(ChatColor.RED + "You need admin permission to stop events!");
            return;
        }

        if (!eventManager.isEventRunning() && !eventManager.isCountdownActive()) {
            player.sendMessage(ChatColor.RED + "No event or countdown is currently running!");
            return;
        }

        if (eventManager.isCountdownActive()) {
            player.sendMessage(ChatColor.YELLOW + "Cancelling countdown...");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Stopping event...");
        }

        eventManager.stopEvent();
    }

    private void handleSetSpawn(Player player) {
        if (!player.hasPermission("meowevent.admin")) {
            player.sendMessage(ChatColor.RED + "You need admin permission to set spawn!");
            return;
        }

        plugin.getConfigManager().setSpawnLocation(player.getLocation());
        player.sendMessage(ChatColor.GREEN + "Event spawn set to your current location!");
        player.sendMessage(ChatColor.GRAY + "World: " + player.getWorld().getName());
        player.sendMessage(ChatColor.GRAY + "X: " + (int)player.getLocation().getX() +
                " Y: " + (int)player.getLocation().getY() +
                " Z: " + (int)player.getLocation().getZ());
    }

    private void handleSetPlayerSpawn(Player player) {
        if (!player.hasPermission("meowevent.admin")) {
            player.sendMessage(ChatColor.RED + "You need admin permission to set player spawn!");
            return;
        }

        plugin.getConfigManager().setPlayerSpawnLocation(player.getLocation());
        player.sendMessage(ChatColor.GREEN + "Player spawn set to your current location!");
        player.sendMessage(ChatColor.GRAY + "Players will be teleported here when they die or leave");
        player.sendMessage(ChatColor.GRAY + "World: " + player.getWorld().getName());
        player.sendMessage(ChatColor.GRAY + "X: " + (int)player.getLocation().getX() +
                " Y: " + (int)player.getLocation().getY() +
                " Z: " + (int)player.getLocation().getZ());
    }

    private void handleSetEventSpawn(Player player) {
        if (!player.hasPermission("meowevent.admin")) {
            player.sendMessage(ChatColor.RED + "You need admin permission to set event spawn!");
            return;
        }

        plugin.getConfigManager().setEventJoinSpawnLocation(player.getLocation());
        player.sendMessage(ChatColor.GREEN + "Event join spawn set to your current location!");
        player.sendMessage(ChatColor.GRAY + "Players will be teleported here when they use /event");
        player.sendMessage(ChatColor.GRAY + "World: " + player.getWorld().getName());
        player.sendMessage(ChatColor.GRAY + "X: " + (int)player.getLocation().getX() +
                " Y: " + (int)player.getLocation().getY() +
                " Z: " + (int)player.getLocation().getZ());
    }

    private void handleTeamSize(Player player, String sizeArg) {
        if (!player.hasPermission("meowevent.admin")) {
            player.sendMessage(ChatColor.RED + "You need admin permission to change team size!");
            return;
        }

        try {
            int size = Integer.parseInt(sizeArg);

            if (size < 1 || size > 5) {
                player.sendMessage(ChatColor.RED + "Team size must be between 1 and 5!");
                player.sendMessage(ChatColor.YELLOW + "1 = Solo (1v1v1...), 2 = 2v2, 3 = 3v3, etc.");
                return;
            }

            eventManager.setTeamSize(size);

            if (size == 1) {
                player.sendMessage(ChatColor.GREEN + "Mode set to Solo (Free-for-all)");
            } else {
                player.sendMessage(ChatColor.GREEN + "Team size set to " + size + "v" + size);
            }

        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number! Use: /meowevent team <1-5>");
        }
    }

    private void handleBorderInterval(Player player, String intervalArg) {
        if (!player.hasPermission("meowevent.admin")) {
            player.sendMessage(ChatColor.RED + "You need admin permission to change border settings!");
            return;
        }

        try {
            int seconds = Integer.parseInt(intervalArg);

            if (seconds < 10) {
                player.sendMessage(ChatColor.RED + "Border interval must be at least 10 seconds!");
                return;
            }

            plugin.getBorderManager().setShrinkInterval(seconds);
            player.sendMessage(ChatColor.GREEN + "Border shrink interval set to " + seconds + " seconds");

        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number! Use: /meowevent border <seconds>");
        }
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("meowevent.admin")) {
            player.sendMessage(ChatColor.RED + "You need admin permission to reload config!");
            return;
        }

        plugin.getConfigManager().reload();
        player.sendMessage(ChatColor.GREEN + "MeowMCEvents config reloaded!");

        if (plugin.getConfigManager().isDebugEnabled()) {
            player.sendMessage(ChatColor.YELLOW + "Debug mode is currently ENABLED");
        } else {
            player.sendMessage(ChatColor.GRAY + "Debug mode is currently disabled");
        }
    }

    private void handleDebugToggle(Player player) {
        if (!player.hasPermission("meowevent.admin")) {
            player.sendMessage(ChatColor.RED + "You need admin permission to toggle debug!");
            return;
        }

        boolean currentState = plugin.getConfigManager().isDebugEnabled();
        plugin.getConfig().set("debug.enabled", !currentState);
        plugin.saveConfig();
        plugin.getConfigManager().reload();

        if (!currentState) {
            player.sendMessage(ChatColor.GREEN + "Debug mode ENABLED - check console for detailed logs");
            plugin.getLogger().info("[DEBUG] Debug mode enabled by " + player.getName());
        } else {
            player.sendMessage(ChatColor.YELLOW + "Debug mode DISABLED");
        }
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "========== " + ChatColor.YELLOW + "MeowMCEvents Help" + ChatColor.GOLD + " ==========");
        player.sendMessage(ChatColor.YELLOW + "/meowevent" + ChatColor.GRAY + " - Open event GUI");
        player.sendMessage(ChatColor.YELLOW + "/meowevent start" + ChatColor.GRAY + " - Start an event");
        player.sendMessage(ChatColor.YELLOW + "/meowevent stop" + ChatColor.GRAY + " - Stop current event");
        player.sendMessage(ChatColor.YELLOW + "/meowevent setspawn" + ChatColor.GRAY + " - Set event arena spawn point");
        player.sendMessage(ChatColor.YELLOW + "/meowevent setevent" + ChatColor.GRAY + " - Set /event join teleport point");
        player.sendMessage(ChatColor.YELLOW + "/meowevent setplayerspawn" + ChatColor.GRAY + " - Set player respawn point");
        player.sendMessage(ChatColor.YELLOW + "/meowevent team <size>" + ChatColor.GRAY + " - Set mode (1=Solo, 2-5=Teams)");
        player.sendMessage(ChatColor.YELLOW + "/meowevent border <seconds>" + ChatColor.GRAY + " - Set border shrink interval");
        player.sendMessage(ChatColor.YELLOW + "/meowevent reload" + ChatColor.GRAY + " - Reload configuration");
        player.sendMessage(ChatColor.YELLOW + "/meowevent debug" + ChatColor.GRAY + " - Toggle debug mode");
        player.sendMessage(ChatColor.YELLOW + "/kits" + ChatColor.GRAY + " - Select event kit");
        player.sendMessage(ChatColor.YELLOW + "/meowevent help" + ChatColor.GRAY + " - Show this help message");
        player.sendMessage(ChatColor.GOLD + "=========================================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("start", "stop", "setspawn", "setplayerspawn", "setevent", "team", "border", "reload", "debug", "help");
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