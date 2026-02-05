package me.oblueberrey.meowMcEvents.commands;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.gui.KitsGUI;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.utils.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KitsCommand implements CommandExecutor {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;

    // Improved RGB Colors
    private static final String GREY = "&#AAAAAA";
    private static final String RED = "&#FF5555";
    private static final String YELLOW = "&#FFE566";
    private static final String ORANGE = "&#FF9944";

    public KitsCommand(MeowMCEvents plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    private String msg(String text) {
        return ConfigManager.colorize(text);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg(GREY + "" + RED + "players only"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("meowevent.admin")) {
            player.sendMessage(msg(GREY + "" + RED + "no permission"));
            return true;
        }

        // Open kit selection GUI
        KitsGUI gui = new KitsGUI(plugin, eventManager);
        gui.openGUI(player);

        return true;
    }
}