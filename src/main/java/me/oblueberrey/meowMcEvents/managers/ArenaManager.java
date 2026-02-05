package me.oblueberrey.meowMcEvents.managers;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaManager {

    private final MeowMCEvents plugin;
    // Thread-safe map for arena storage
    private final Map<String, Arena> arenas;
    private volatile String activeArenaName;
    private File arenasFile;
    private FileConfiguration arenasConfig;

    public ArenaManager(MeowMCEvents plugin) {
        this.plugin = plugin;
        this.arenas = new ConcurrentHashMap<>();
        this.activeArenaName = "";
    }

    public void loadArenas() {
        arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        if (!arenasFile.exists()) {
            plugin.saveResource("arenas.yml", false);
        }
        arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);

        arenas.clear();
        activeArenaName = arenasConfig.getString("active-arena", "");

        ConfigurationSection arenasSection = arenasConfig.getConfigurationSection("arenas");
        if (arenasSection == null) return;

        for (String name : arenasSection.getKeys(false)) {
            ConfigurationSection arenaSection = arenasSection.getConfigurationSection(name);
            if (arenaSection == null) continue;

            Arena arena = new Arena(name);

            // Load pos1
            if (arenaSection.contains("pos1")) {
                String worldName = arenaSection.getString("pos1.world", "world");
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    double x = arenaSection.getDouble("pos1.x");
                    double y = arenaSection.getDouble("pos1.y");
                    double z = arenaSection.getDouble("pos1.z");
                    arena.setPos1(new Location(world, x, y, z));
                }
            }

            // Load pos2
            if (arenaSection.contains("pos2")) {
                String worldName = arenaSection.getString("pos2.world", "world");
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    double x = arenaSection.getDouble("pos2.x");
                    double y = arenaSection.getDouble("pos2.y");
                    double z = arenaSection.getDouble("pos2.z");
                    arena.setPos2(new Location(world, x, y, z));
                }
            }

            arenas.put(name.toLowerCase(), arena);
        }

        plugin.getLogger().info("[Arenas] Loaded " + arenas.size() + " arena(s). Active: " +
                (activeArenaName.isEmpty() ? "none" : activeArenaName));
    }

    public void saveArenas() {
        if (arenasConfig == null || arenasFile == null) return;

        arenasConfig.set("arenas", null);
        arenasConfig.set("active-arena", activeArenaName);

        for (Map.Entry<String, Arena> entry : arenas.entrySet()) {
            String path = "arenas." + entry.getKey();
            Arena arena = entry.getValue();

            if (arena.getPos1() != null && arena.getPos1().getWorld() != null) {
                arenasConfig.set(path + ".pos1.world", arena.getPos1().getWorld().getName());
                arenasConfig.set(path + ".pos1.x", arena.getPos1().getX());
                arenasConfig.set(path + ".pos1.y", arena.getPos1().getY());
                arenasConfig.set(path + ".pos1.z", arena.getPos1().getZ());
            }

            if (arena.getPos2() != null && arena.getPos2().getWorld() != null) {
                arenasConfig.set(path + ".pos2.world", arena.getPos2().getWorld().getName());
                arenasConfig.set(path + ".pos2.x", arena.getPos2().getX());
                arenasConfig.set(path + ".pos2.y", arena.getPos2().getY());
                arenasConfig.set(path + ".pos2.z", arena.getPos2().getZ());
            }
        }

        try {
            arenasConfig.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[Arenas] Failed to save arenas.yml: " + e.getMessage());
        }
    }

    public boolean createArena(String name) {
        String key = name.toLowerCase();
        if (arenas.containsKey(key)) return false;
        arenas.put(key, new Arena(name));
        saveArenas();
        return true;
    }

    public boolean deleteArena(String name) {
        String key = name.toLowerCase();
        if (!arenas.containsKey(key)) return false;
        arenas.remove(key);
        if (activeArenaName.equalsIgnoreCase(name)) {
            activeArenaName = "";
        }
        saveArenas();
        return true;
    }

    public boolean setPos1(String name, Location loc) {
        String key = name.toLowerCase();
        Arena arena = arenas.get(key);
        if (arena == null) return false;

        // Validate same world if pos2 is set
        if (arena.getPos2() != null && arena.getPos2().getWorld() != null
                && loc.getWorld() != null && !loc.getWorld().equals(arena.getPos2().getWorld())) {
            return false;
        }

        arena.setPos1(loc);
        saveArenas();
        return true;
    }

    public boolean setPos2(String name, Location loc) {
        String key = name.toLowerCase();
        Arena arena = arenas.get(key);
        if (arena == null) return false;

        // Validate same world if pos1 is set
        if (arena.getPos1() != null && arena.getPos1().getWorld() != null
                && loc.getWorld() != null && !loc.getWorld().equals(arena.getPos1().getWorld())) {
            return false;
        }

        arena.setPos2(loc);
        saveArenas();
        return true;
    }

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public Set<String> getArenaNames() {
        return new HashSet<>(arenas.keySet());
    }

    public void setActiveArena(String name) {
        this.activeArenaName = name.toLowerCase();
        saveArenas();
    }

    public String getActiveArenaName() {
        return activeArenaName;
    }

    public Arena getActiveArena() {
        if (activeArenaName.isEmpty()) return null;
        return arenas.get(activeArenaName);
    }

    public boolean isInsideActiveArena(Location loc) {
        Arena arena = getActiveArena();
        if (arena == null || !arena.isComplete()) return true; // No arena = no restriction
        return arena.contains(loc);
    }

    /**
     * Represents a cuboid arena defined by two corner positions
     */
    public static class Arena {
        private final String name;
        private Location pos1;
        private Location pos2;

        public Arena(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Location getPos1() {
            return pos1;
        }

        public void setPos1(Location pos1) {
            this.pos1 = pos1;
        }

        public Location getPos2() {
            return pos2;
        }

        public void setPos2(Location pos2) {
            this.pos2 = pos2;
        }

        public boolean isComplete() {
            return pos1 != null && pos2 != null
                    && pos1.getWorld() != null && pos2.getWorld() != null;
        }

        public boolean contains(Location loc) {
            if (!isComplete()) return true;
            if (loc == null || loc.getWorld() == null) return false;
            if (!loc.getWorld().equals(pos1.getWorld())) return false;

            int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
            int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
            int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
            int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
            int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
            int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();

            return x >= minX && x <= maxX
                    && y >= minY && y <= maxY
                    && z >= minZ && z <= maxZ;
        }

        public int getSizeX() {
            if (!isComplete()) return 0;
            return Math.abs(pos1.getBlockX() - pos2.getBlockX()) + 1;
        }

        public int getSizeY() {
            if (!isComplete()) return 0;
            return Math.abs(pos1.getBlockY() - pos2.getBlockY()) + 1;
        }

        public int getSizeZ() {
            if (!isComplete()) return 0;
            return Math.abs(pos1.getBlockZ() - pos2.getBlockZ()) + 1;
        }

        /**
         * Get the minimum distance from a location to any edge of the arena.
         * Returns 0 if at the very edge, higher values = further inside.
         * Returns -1 if outside the arena or arena is incomplete.
         */
        public int getDistanceFromEdge(Location loc) {
            if (!isComplete() || loc == null) return -1;
            if (!loc.getWorld().equals(pos1.getWorld())) return -1;

            int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
            int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
            int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
            int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

            int x = loc.getBlockX();
            int z = loc.getBlockZ();

            // Distance from each wall
            int distFromMinX = x - minX;
            int distFromMaxX = maxX - x;
            int distFromMinZ = z - minZ;
            int distFromMaxZ = maxZ - z;

            // Minimum distance to any wall (XZ plane only - Y is ignored for gameplay)
            int minDist = Math.min(Math.min(distFromMinX, distFromMaxX),
                    Math.min(distFromMinZ, distFromMaxZ));

            return Math.max(0, minDist);
        }
    }
}
