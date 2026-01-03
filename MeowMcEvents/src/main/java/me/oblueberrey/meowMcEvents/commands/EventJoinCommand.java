package me.oblueberrey.meowMcEvents.commands;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EventJoinCommand implements CommandExecutor {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;

    public EventJoinCommand(MeowMCEvents plugin, EventManager eventManager) {
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

        // Check if event is running
        if (eventManager.isEventRunning()) {
            player.sendMessage(ChatColor.RED + "Event is already running! Wait for the next one.");
            return true;
        }

        // Check if already joined
        if (eventManager.hasPlayerJoined(player)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfigManager().getMessage("already-joined")));
            return true;
        }

        // Add player to event queue
        eventManager.addPlayer(player);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfigManager().getMessage("joined-event")));

        return true;
    }
}