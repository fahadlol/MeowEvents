package me.oblueberrey.meowMcEvents.listeners;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.utils.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PvPListener implements Listener {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;

    public PvPListener(MeowMCEvents plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!eventManager.isEventRunning()) {
            return;
        }

        // Check if victim is a player in the event
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        if (!eventManager.isPlayerInEvent(victim)) {
            return;
        }

        // Get the attacker (could be direct or via projectile)
        Player attacker = null;

        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            }
        }

        // If no player attacker, allow the damage
        if (attacker == null) {
            return;
        }

        // Check if attacker is in the event
        if (!eventManager.isPlayerInEvent(attacker)) {
            return;
        }

        // Check self-damage (ender pearls, own explosions, etc.)
        if (attacker.equals(victim)) {
            if (!plugin.getConfigManager().isSelfDamageAllowed()) {
                event.setCancelled(true);
                return;
            }
            // Self-damage is allowed, skip other checks
            return;
        }

        // Block PvP during grace period
        if (eventManager.isGracePeriodActive()) {
            event.setCancelled(true);
            attacker.sendMessage(ConfigManager.colorize("&#AAAAAA&#FF5555grace period active"));
            return;
        }

        // Block Friendly Fire
        if (!plugin.getConfigManager().isFriendlyFireAllowed()) {
            if (plugin.getTeamManager().isSameTeam(attacker, victim)) {
                event.setCancelled(true);
                attacker.sendMessage(ConfigManager.colorize("&#AAAAAA&#FF5555you cannot hit teammates"));
                return;
            }
        }
    }
}
