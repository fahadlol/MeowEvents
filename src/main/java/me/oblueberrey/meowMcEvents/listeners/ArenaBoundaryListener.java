package me.oblueberrey.meowMcEvents.listeners;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.ArenaManager;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import me.oblueberrey.meowMcEvents.utils.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class ArenaBoundaryListener {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;
    private final ArenaManager arenaManager;
    private BukkitTask boundaryTask;

    public ArenaBoundaryListener(MeowMCEvents plugin, EventManager eventManager, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.arenaManager = arenaManager;
    }

    public void startBoundaryCheck() {
        stopBoundaryCheck();

        ArenaManager.Arena activeArena = arenaManager.getActiveArena();
        if (activeArena == null || !activeArena.isComplete()) return;

        int interval = plugin.getConfigManager().getArenaBoundaryCheckInterval();

        boundaryTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!eventManager.isEventRunning()) {
                stopBoundaryCheck();
                return;
            }

            ArenaManager.Arena arena = arenaManager.getActiveArena();
            if (arena == null || !arena.isComplete()) return;

            int damageZone = plugin.getConfigManager().getArenaDamageZoneSize();
            double maxDamage = plugin.getConfigManager().getArenaDamageZoneMaxDamage();

            // Check alive players - apply damage zone + boundary handling
            for (UUID uuid : eventManager.getAlivePlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) continue;

                if (!arena.contains(player.getLocation())) {
                    // Outside arena - check how far
                    int distanceOutside = getDistanceOutsideArena(arena, player.getLocation());

                    if (distanceOutside >= 30) {
                        // Far outside (30+ blocks) - kill them
                        if (plugin.getKillFeedManager() != null) {
                            plugin.getKillFeedManager().broadcastBorderDeath(player);
                        }
                        player.setHealth(0);
                    } else {
                        // Just outside - teleport back
                        teleportBackToArena(player);
                    }
                } else if (damageZone > 0) {
                    // Inside arena - check if in damage zone
                    int distFromEdge = arena.getDistanceFromEdge(player.getLocation());
                    if (distFromEdge < damageZone) {
                        // Calculate damage: closer to edge = more damage
                        // distFromEdge=0 means at the very edge, distFromEdge=damageZone-1 means just entered zone
                        double intensity = 1.0 - ((double) distFromEdge / damageZone);
                        double damage = maxDamage * intensity;
                        if (damage < 0.5) damage = 0.5;

                        player.damage(damage);
                        player.sendActionBar(ConfigManager.colorize(
                                "&#FF5555\u26A0 Border damage! Move inward!"));

                        // Warning sound at low intensity, louder near edge
                        if (intensity > 0.5) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS,
                                    (float) intensity, 0.5f);
                        }
                    }
                }
            }

            // Check spectators - only teleport, no damage
            for (UUID uuid : eventManager.getSpectators()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) continue;
                if (!arena.contains(player.getLocation())) {
                    teleportBackToArena(player);
                }
            }
        }, interval, interval);
    }

    public void stopBoundaryCheck() {
        if (boundaryTask != null) {
            boundaryTask.cancel();
            boundaryTask = null;
        }
    }

    public boolean isActive() {
        return boundaryTask != null;
    }

    private void teleportBackToArena(Player player) {
        Location spawn = plugin.getConfigManager().getSpawnLocation();
        if (spawn != null && spawn.getWorld() != null) {
            player.teleport(spawn);
            player.sendMessage(ConfigManager.colorize("&#AAAAAA&#FF5555You left the arena boundary."));
        }
    }

    /**
     * Calculate how far outside the arena a location is (in blocks).
     * Returns 0 if inside or on edge.
     */
    private int getDistanceOutsideArena(ArenaManager.Arena arena, Location loc) {
        if (arena == null || !arena.isComplete() || loc == null) return 0;

        Location pos1 = arena.getPos1();
        Location pos2 = arena.getPos2();

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        int distX = 0;
        int distZ = 0;

        if (x < minX) distX = minX - x;
        else if (x > maxX) distX = x - maxX;

        if (z < minZ) distZ = minZ - z;
        else if (z > maxZ) distZ = z - maxZ;

        // Return the maximum distance on either axis
        return Math.max(distX, distZ);
    }
}
