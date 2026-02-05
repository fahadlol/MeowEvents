package me.oblueberrey.meowMcEvents.commands;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.ArenaManager;
import me.oblueberrey.meowMcEvents.utils.MessageUtils;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ArenaCommand implements CommandExecutor, TabCompleter {

    private final MeowMCEvents plugin;
    private final ArenaManager arenaManager;

    public ArenaCommand(MeowMCEvents plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("meowevent.admin")) {
            MessageUtils.sendError(player, "No permission.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                handleCreate(player, args);
                break;
            case "pos1":
                handlePos1(player, args);
                break;
            case "pos2":
                handlePos2(player, args);
                break;
            case "delete":
                handleDelete(player, args);
                break;
            case "list":
                handleList(player);
                break;
            case "set":
                handleSet(player, args);
                break;
            case "info":
                handleInfo(player, args);
                break;
            default:
                sendUsage(player);
                break;
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendError(player, "Usage: /arena create <name>");
            return;
        }
        String name = args[1].toLowerCase();
        if (!name.matches("[a-zA-Z0-9_-]+")) {
            MessageUtils.sendError(player, "Arena name can only contain letters, numbers, underscores, and hyphens.");
            return;
        }
        if (arenaManager.createArena(name)) {
            MessageUtils.sendSuccess(player, "Arena &#FFE566" + name + " &#AAAAAAcreated. Set pos1 and pos2 to define boundaries.");
        } else {
            MessageUtils.sendError(player, "Arena &#FFE566" + name + " &#FF5555already exists.");
        }
    }

    private void handlePos1(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendError(player, "Usage: /arena pos1 <name>");
            return;
        }
        String name = args[1].toLowerCase();
        Location loc = player.getLocation();

        if (arenaManager.getArena(name) == null) {
            MessageUtils.sendError(player, "Arena &#FFE566" + name + " &#FF5555does not exist.");
            return;
        }

        if (arenaManager.setPos1(name, loc)) {
            MessageUtils.sendSuccess(player, "Pos1 set for &#FFE566" + name + " &#AAAAAAat " +
                    loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        } else {
            MessageUtils.sendError(player, "Failed to set pos1. Ensure both positions are in the same world.");
        }
    }

    private void handlePos2(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendError(player, "Usage: /arena pos2 <name>");
            return;
        }
        String name = args[1].toLowerCase();
        Location loc = player.getLocation();

        if (arenaManager.getArena(name) == null) {
            MessageUtils.sendError(player, "Arena &#FFE566" + name + " &#FF5555does not exist.");
            return;
        }

        if (arenaManager.setPos2(name, loc)) {
            MessageUtils.sendSuccess(player, "Pos2 set for &#FFE566" + name + " &#AAAAAAat " +
                    loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        } else {
            MessageUtils.sendError(player, "Failed to set pos2. Ensure both positions are in the same world.");
        }
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendError(player, "Usage: /arena delete <name>");
            return;
        }
        String name = args[1].toLowerCase();
        if (arenaManager.deleteArena(name)) {
            MessageUtils.sendSuccess(player, "Arena &#FFE566" + name + " &#AAAAAAdeleted.");
        } else {
            MessageUtils.sendError(player, "Arena &#FFE566" + name + " &#FF5555does not exist.");
        }
    }

    private void handleList(Player player) {
        Set<String> names = arenaManager.getArenaNames();
        if (names.isEmpty()) {
            MessageUtils.sendInfo(player, "No arenas defined. Use &#FFE566/arena create <name>");
            return;
        }

        player.sendMessage(MessageUtils.colorize("&#666666--- &#FFE566Arenas &#666666---"));
        String activeName = arenaManager.getActiveArenaName();
        for (String name : names) {
            ArenaManager.Arena arena = arenaManager.getArena(name);
            String status = arena.isComplete() ? "&#55FF55complete" : "&#FF5555incomplete";
            String active = name.equalsIgnoreCase(activeName) ? " &#FFD700[ACTIVE]" : "";
            player.sendMessage(MessageUtils.colorize(" &#AAAAAA- &#FFE566" + name + " &#AAAAAA(" + status + "&#AAAAAA)" + active));
        }
    }

    private void handleSet(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendError(player, "Usage: /arena set <name>");
            return;
        }
        String name = args[1].toLowerCase();
        ArenaManager.Arena arena = arenaManager.getArena(name);
        if (arena == null) {
            MessageUtils.sendError(player, "Arena &#FFE566" + name + " &#FF5555does not exist.");
            return;
        }
        if (!arena.isComplete()) {
            MessageUtils.sendError(player, "Arena &#FFE566" + name + " &#FF5555is incomplete. Set both pos1 and pos2 first.");
            return;
        }
        arenaManager.setActiveArena(name);
        MessageUtils.sendSuccess(player, "Active arena set to &#FFE566" + name);
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendError(player, "Usage: /arena info <name>");
            return;
        }
        String name = args[1].toLowerCase();
        ArenaManager.Arena arena = arenaManager.getArena(name);
        if (arena == null) {
            MessageUtils.sendError(player, "Arena &#FFE566" + name + " &#FF5555does not exist.");
            return;
        }

        player.sendMessage(MessageUtils.colorize("&#666666--- &#FFE566Arena: " + name + " &#666666---"));

        if (arena.getPos1() != null) {
            Location p1 = arena.getPos1();
            player.sendMessage(MessageUtils.colorize(" &#AAAAAAPos1: &#FFE566" +
                    p1.getBlockX() + ", " + p1.getBlockY() + ", " + p1.getBlockZ() +
                    " &#AAAAAA(" + p1.getWorld().getName() + ")"));
        } else {
            player.sendMessage(MessageUtils.colorize(" &#AAAAAAPos1: &#FF5555not set"));
        }

        if (arena.getPos2() != null) {
            Location p2 = arena.getPos2();
            player.sendMessage(MessageUtils.colorize(" &#AAAAAAPos2: &#FFE566" +
                    p2.getBlockX() + ", " + p2.getBlockY() + ", " + p2.getBlockZ() +
                    " &#AAAAAA(" + p2.getWorld().getName() + ")"));
        } else {
            player.sendMessage(MessageUtils.colorize(" &#AAAAAAPos2: &#FF5555not set"));
        }

        if (arena.isComplete()) {
            player.sendMessage(MessageUtils.colorize(" &#AAAAAASize: &#FFE566" +
                    arena.getSizeX() + "x" + arena.getSizeY() + "x" + arena.getSizeZ()));
        }

        String activeName = arenaManager.getActiveArenaName();
        if (name.equalsIgnoreCase(activeName)) {
            player.sendMessage(MessageUtils.colorize(" &#FFD700[ACTIVE]"));
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(MessageUtils.colorize("&#666666--- &#FFE566/arena &#666666---"));
        player.sendMessage(MessageUtils.colorize(" &#FFE566/arena create <name> &#AAAAAA- create arena"));
        player.sendMessage(MessageUtils.colorize(" &#FFE566/arena pos1 <name> &#AAAAAA- set corner 1"));
        player.sendMessage(MessageUtils.colorize(" &#FFE566/arena pos2 <name> &#AAAAAA- set corner 2"));
        player.sendMessage(MessageUtils.colorize(" &#FFE566/arena set <name> &#AAAAAA- set active arena"));
        player.sendMessage(MessageUtils.colorize(" &#FFE566/arena delete <name> &#AAAAAA- delete arena"));
        player.sendMessage(MessageUtils.colorize(" &#FFE566/arena list &#AAAAAA- list all arenas"));
        player.sendMessage(MessageUtils.colorize(" &#FFE566/arena info <name> &#AAAAAA- show arena info"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("create", "pos1", "pos2", "delete", "list", "set", "info");
            String input = args[0].toLowerCase();
            for (String sub : subcommands) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("pos1") || sub.equals("pos2") || sub.equals("delete") || sub.equals("set") || sub.equals("info")) {
                String input = args[1].toLowerCase();
                for (String name : arenaManager.getArenaNames()) {
                    if (name.startsWith(input)) {
                        completions.add(name);
                    }
                }
            }
        }

        return completions;
    }
}
