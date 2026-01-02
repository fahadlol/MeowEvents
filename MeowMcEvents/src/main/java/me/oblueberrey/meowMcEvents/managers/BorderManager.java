package me.oblueberrey.meowMcEvents.managers;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.scheduler.BukkitTask;

public class BorderManager {

    private final MeowMCEvents plugin;
    private BukkitTask shrinkTask;
    private int startSize;
    private int minSize;
    private int shrinkInterval;
    private int currentSize;
    private double originalBorderSize;
    private Location originalCenter;

    public BorderManager(MeowMCEvents plugin) {
        this.plugin = plugin;
        this.startSize = plugin.getConfigManager().getBorderStartSize();
        this.minSize = plugin.getConfigManager().getBorderShrinkTo();
        this.shrinkInterval = plugin.getConfigManager().getBorderIntervalSeconds();
        this.currentSize = startSize;
    }

    /**
     * Start the border shrinking task
     * Centers border at spawn location and begins shrinking every interval
     */
    public void startBorderShrink(World world, Location center) {
        if (world == null || center == null) {
            plugin.getLogger().warning("Cannot start border shrink: world or center is null");
            return;
        }

        // Stop any existing task
        stopBorderShrink();

        // Save original border settings
        WorldBorder border = world.getWorldBorder();
        originalBorderSize = border.getSize();
        originalCenter = border.getCenter();

        // Set initial border
        border.setCenter(center);
        border.setSize(startSize);
        currentSize = startSize;

        plugin.getLogger().info("Border initialized at " + startSize + "x" + startSize);

        // Calculate shrink amount per interval
        int totalShrinks = (startSize - minSize) / 10; // Shrink by 10 blocks each time
        if (totalShrinks <= 0) totalShrinks = 1;

        final int shrinkAmount = Math.max(10, (startSize - minSize) / totalShrinks);

        // Start repeating task (runs every X seconds)
        shrinkTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (currentSize > minSize) {
                currentSize = Math.max(minSize, currentSize - shrinkAmount);

                // Smooth shrink over the interval duration (prevents teleport glitches)
                border.setSize(currentSize, shrinkInterval);

                // Broadcast message
                String message = plugin.getConfigManager().getMessage("border-shrink")
                        .replace("%size%", String.valueOf(currentSize));
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));

                plugin.getLogger().info("Border shrinking to " + currentSize + "x" + currentSize);
            } else {
                // Minimum size reached, keep border at minimum
                border.setSize(minSize);
                plugin.getLogger().info("Border reached minimum size: " + minSize);
            }
        }, shrinkInterval * 20L, shrinkInterval * 20L); // Convert seconds to ticks
    }

    /**
     * Stop border shrinking task
     */
    public void stopBorderShrink() {
        if (shrinkTask != null) {
            shrinkTask.cancel();
            shrinkTask = null;
            plugin.getLogger().info("Border shrinking task stopped");
        }
    }

    /**
     * Reset border to original world settings
     * Called when event stops
     */
    public void resetBorder(World world) {
        if (world == null) {
            plugin.getLogger().warning("Cannot reset border: world is null");
            return;
        }

        stopBorderShrink();

        WorldBorder border = world.getWorldBorder();

        // Restore original settings or use default
        if (originalCenter != null) {
            border.setCenter(originalCenter);
        } else {
            border.setCenter(0, 0);
        }

        if (originalBorderSize > 0) {
            border.setSize(originalBorderSize);
        } else {
            border.setSize(59999968); // Default Minecraft border size
        }

        currentSize = startSize;
        plugin.getLogger().info("Border reset to default settings");
    }

    /**
     * Set shrink interval in seconds
     * Can be changed via command
     */
    public void setShrinkInterval(int seconds) {
        if (seconds < 10) {
            plugin.getLogger().warning("Shrink interval too low, setting to minimum 10 seconds");
            seconds = 10; // Minimum 10 seconds to prevent lag
        }
        this.shrinkInterval = seconds;
        plugin.getLogger().info("Border shrink interval set to " + seconds + " seconds");
    }

    /**
     * Get current border size
     */
    public int getCurrentSize() {
        return currentSize;
    }

    /**
     * Get shrink interval
     */
    public int getShrinkInterval() {
        return shrinkInterval;
    }

    /**
     * Check if border is currently shrinking
     */
    public boolean isShrinking() {
        return shrinkTask != null;
    }

    /**
     * Get start size from config
     */
    public int getStartSize() {
        return startSize;
    }

    /**
     * Get minimum size from config
     */
    public int getMinSize() {
        return minSize;
    }

    /**
     * Force set border to specific size (emergency use)
     */
    public void setBorderSize(World world, int size) {
        if (world == null) return;

        WorldBorder border = world.getWorldBorder();
        border.setSize(size);
        currentSize = size;
        plugin.getLogger().info("Border manually set to " + size + "x" + size);
    }
}