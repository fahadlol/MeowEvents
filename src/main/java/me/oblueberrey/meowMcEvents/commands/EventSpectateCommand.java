package me.oblueberrey.meowMcEvents.commands;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.utils.MessageUtils;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EventSpectateCommand implements CommandExecutor {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;

    public EventSpectateCommand(MeowMCEvents plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("meowevent.spectate")) {
            MessageUtils.sendError(player, "No permission.");
            return true;
        }

        if (!eventManager.isEventRunning()) {
            MessageUtils.sendError(player, "No event is currently running.");
            return true;
        }

        if (eventManager.isPlayerInEvent(player)) {
            MessageUtils.sendError(player, "You are currently participating in this event.");
            return true;
        }

        if (eventManager.isSpectator(player)) {
            MessageUtils.sendError(player, "You are already spectating.");
            return true;
        }

        // Add as spectator
        eventManager.addSpectator(player);

        // Teleport to event spawn
        Location eventSpawn = plugin.getConfigManager().getSpawnLocation();
        if (eventSpawn != null && eventSpawn.getWorld() != null) {
            player.teleport(eventSpawn);
        }

        MessageUtils.sendInfo(player, "You are now spectating. Use &#FFE566/leave &#AAAAAAto exit.");
        return true;
    }
}
