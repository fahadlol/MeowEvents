package me.oblueberrey.meowMcEvents.utils;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.UUID;

/**
 * Handles all event feedback: sounds, action bar messages, particles, and boss bar
 * Uses bright yellow/orange RGB colors with grey emojis
 */
public class EventFeedback {

    private final MeowMCEvents plugin;
    private BossBar eventBossBar;
    private BukkitTask bossBarUpdateTask;

    // Improved RGB Color Palette - Vibrant and modern
    private static final String PRIMARY = "&#FFE566";      // Soft gold
    private static final String YELLOW = "&#FFE566";       // Soft gold (alias)
    private static final String BRIGHT_YELLOW = "&#FFFF55"; // Bright yellow
    private static final String ORANGE = "&#FF9944";       // Warm orange
    private static final String LIGHT_ORANGE = "&#FFBB66"; // Light orange
    private static final String GREY = "&#AAAAAA";         // Grey for text
    private static final String DARK_GREY = "&#666666";    // Dark grey for lines
    private static final String WHITE = "&#FFFFFF";        // White
    private static final String RED = "&#FF5555";          // Bright red
    private static final String GREEN = "&#55FF55";        // Bright green
    private static final String AQUA = "&#55FFFF";         // Aqua/Cyan
    private static final String PINK = "&#FF7EB3";         // Pink accent

    // Key symbols (used sparingly)
    private static final String SWORD = "\u2694";   // Combat/event start
    private static final String SKULL = "\u2620";   // Deaths
    private static final String STAR = "\u2B50";    // Victory/winner
    private static final String ARROW = "\u27A4";   // Arrow

    private static final String GOLD = MessageUtils.GOLD;
    private static final String BORDER_COLOR = MessageUtils.BORDER_COLOR;

    // Icons
    private static final String GEAR = "\u2699";

    public EventFeedback(MeowMCEvents plugin) {
        this.plugin = plugin;
    }

    /**
     * Send a left-aligned message with decorative lines
     */
    public void broadcastAnnouncement(String title, String... lines) {
        String coloredTitle = colorize(title);

        String[] coloredLines = new String[lines.length];
        for (int i = 0; i < lines.length; i++) {
            coloredLines[i] = colorize(lines[i]);
        }

        String line = MessageUtils.getLine(40, BORDER_COLOR);

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(line);
        Bukkit.broadcastMessage(coloredTitle);
        for (String coloredLine : coloredLines) {
            Bukkit.broadcastMessage(coloredLine);
        }
        Bukkit.broadcastMessage(line);
        Bukkit.broadcastMessage("");
    }

    /**
     * Send a more compact left-aligned announcement
     */
    public void broadcastSmallAnnouncement(String title, String subtitle) {
        String coloredTitle = colorize(title);
        String coloredSubtitle = subtitle != null ? colorize(subtitle) : null;

        String line = MessageUtils.getLine(40, BORDER_COLOR);

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(line);
        Bukkit.broadcastMessage(coloredTitle);
        if (coloredSubtitle != null) {
            Bukkit.broadcastMessage(coloredSubtitle);
        }
        Bukkit.broadcastMessage(line);
        Bukkit.broadcastMessage("");
    }

    /**
     * Translate hex color codes to chat colors
     */
    public static String colorize(String message) {
        return MessageUtils.colorize(message);
    }

    /**
     * Format a styled message with emojis
     */
    public String styled(String emoji, String message) {
        return colorize(GREY + emoji + " " + BRIGHT_YELLOW + message);
    }

    /**
     * Format a styled message with emoji in grey, text in yellow/orange
     */
    public String styledOrange(String emoji, String message) {
        return colorize(GREY + emoji + " " + ORANGE + message);
    }

    // ==================== BOSS BAR ====================

    /**
     * Create and show the event boss bar
     */
    public void createBossBar(String title, BarColor color) {
        if (eventBossBar != null) {
            eventBossBar.removeAll();
        }
        eventBossBar = Bukkit.createBossBar(
                ChatColor.translateAlternateColorCodes('&', title),
                color,
                BarStyle.SOLID
        );
        eventBossBar.setVisible(true);
    }

    /**
     * Add a player to the boss bar
     */
    public void addPlayerToBossBar(Player player) {
        if (eventBossBar != null && player != null) {
            eventBossBar.addPlayer(player);
        }
    }

    /**
     * Remove a player from the boss bar
     */
    public void removePlayerFromBossBar(Player player) {
        if (eventBossBar != null && player != null) {
            eventBossBar.removePlayer(player);
        }
    }

    /**
     * Update boss bar title and progress
     */
    public void updateBossBar(String title, double progress, BarColor color) {
        if (eventBossBar != null) {
            eventBossBar.setTitle(ChatColor.translateAlternateColorCodes('&', title));
            eventBossBar.setProgress(Math.max(0, Math.min(1, progress)));
            eventBossBar.setColor(color);
        }
    }

    /**
     * Start updating boss bar with player count
     */
    public void startBossBarUpdates(Set<UUID> alivePlayers, Set<UUID> spectators, int totalPlayers) {
        stopBossBarUpdates();

        bossBarUpdateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (eventBossBar == null) return;

            int alive = alivePlayers.size();
            double progress = totalPlayers > 0 ? (double) alive / totalPlayers : 0;

            String title = colorize(GREY + "\u2694 " + BRIGHT_YELLOW + alive + " " + toSmallCaps("alive") + " " + GREY + "\u2694");
            updateBossBar(title, progress, alive <= 3 ? BarColor.RED : BarColor.GREEN);
        }, 0L, 20L); // Update every second
    }

    private String toSmallCaps(String text) {
        return MessageUtils.toSmallCaps(text);
    }

    /**
     * Stop boss bar updates
     */
    public void stopBossBarUpdates() {
        if (bossBarUpdateTask != null) {
            bossBarUpdateTask.cancel();
            bossBarUpdateTask = null;
        }
    }

    /**
     * Remove the boss bar completely
     */
    public void removeBossBar() {
        stopBossBarUpdates();
        if (eventBossBar != null) {
            eventBossBar.removeAll();
            eventBossBar = null;
        }
    }

    // ==================== SOUNDS ====================

    /**
     * Play sound to a specific player
     */
    public void playSound(Player player, Sound sound, float volume, float pitch) {
        if (player != null && player.isOnline()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    /**
     * Play sound to all online players
     */
    public void playSoundAll(Sound sound, float volume, float pitch) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    /**
     * Play countdown tick sound
     */
    public void playCountdownTick(Player player, int secondsLeft) {
        if (secondsLeft <= 5) {
            playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        } else if (secondsLeft <= 10) {
            playSound(player, Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 1.5f);
        } else if (secondsLeft % 10 == 0) {
            playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 1.0f);
        }
    }

    /**
     * Play event start sound
     */
    public void playEventStart(Player player) {
        playSound(player, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);
        Bukkit.getScheduler().runTaskLater(plugin, () ->
            playSound(player, Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f), 10L);
    }

    /**
     * Play player join sound
     */
    public void playPlayerJoin(Player player) {
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
    }

    /**
     * Play kill sound to killer
     */
    public void playKillSound(Player killer) {
        playSound(killer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.5f);
        Bukkit.getScheduler().runTaskLater(plugin, () ->
            playSound(killer, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f), 3L);
    }

    /**
     * Play death sound to victim and nearby players
     */
    public void playDeathSound(Player victim) {
        if (victim == null || !victim.isOnline()) return;
        Location loc = victim.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        // Victim hears a deep thud
        playSound(victim, Sound.ENTITY_PLAYER_DEATH, 1.0f, 0.7f);
        playSound(victim, Sound.ENTITY_WITHER_HURT, 0.6f, 0.6f);

        // Nearby players hear the death
        for (Player nearby : world.getPlayers()) {
            if (nearby.equals(victim)) continue;
            if (nearby.getLocation().distanceSquared(loc) <= 2500) { // 50 blocks
                nearby.playSound(loc, Sound.ENTITY_PLAYER_DEATH, 0.5f, 0.8f);
            }
        }
    }

    /**
     * Play winner celebration sound - layered fanfare
     */
    public void playWinnerSound(Player winner) {
        if (winner == null || !winner.isOnline()) return;

        // Immediate victory fanfare
        playSound(winner, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        playSound(winner, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);

        // Delayed firework sounds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            playSound(winner, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f);
        }, 15L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            playSound(winner, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.8f, 1.2f);
        }, 30L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            playSound(winner, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, 0.7f, 1.0f);
        }, 50L);
    }

    /**
     * Play border warning sound
     */
    public void playBorderWarning(Player player) {
        playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
    }

    // ==================== MESSAGES (No Titles - Using Action Bar & Chat) ====================

    /**
     * Send styled message to player (replaces titles)
     */
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        // Titles/subtitles removed - no action bar spam during countdown
    }

    /**
     * Send message to all players in a set
     */
    public void sendTitleToAll(Set<UUID> players, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
            }
        }
    }

    /**
     * Send countdown message
     */
    public void sendCountdownTitle(Player player, int seconds) {
        // Action bar removed per request
    }

    /**
     * Send kill notification to killer
     */
    public void sendKillTitle(Player killer, String victimName, int killCount) {
        // Action bar removed per request
    }

    /**
     * Send death notification to victim
     */
    public void sendDeathTitle(Player victim, String killerName) {
        // Action bar removed per request
    }

    /**
     * Send winner notification
     */
    public void sendWinnerTitle(Player winner) {
        if (winner == null || !winner.isOnline()) return;
        winner.sendMessage(colorize(GREY + STAR + " " + BRIGHT_YELLOW + "victory " + ORANGE + "you won the event"));
    }

    /**
     * Send team winner notification
     */
    public void sendTeamWinnerTitle(Player player, int teamNumber, org.bukkit.ChatColor teamColor) {
        if (player == null || !player.isOnline()) return;
        player.sendMessage(colorize(GREY + STAR + " " + BRIGHT_YELLOW + "victory " + ORANGE + "your team won"));
    }

    /**
     * Send spectator notification
     */
    public void sendSpectatorTitle(Player player) {
        // Action bar removed per request
    }

    // ==================== PARTICLES ====================

    /**
     * Spawn particles at location
     */
    public void spawnParticles(Location location, Particle particle, int count, double offsetX, double offsetY, double offsetZ) {
        if (location != null && location.getWorld() != null) {
            location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ);
        }
    }

    /**
     * Spawn kill particles at victim location - burst + lingering soul rise
     */
    public void spawnKillParticles(Location location) {
        if (location == null || location.getWorld() == null) return;

        World world = location.getWorld();
        Location particleLoc = location.clone().add(0, 1, 0);

        // Initial burst - damage indicators + crits spreading outward
        world.spawnParticle(Particle.DAMAGE_INDICATOR, particleLoc, 20, 0.4, 0.5, 0.4, 0.1);
        world.spawnParticle(Particle.CRIT, particleLoc, 25, 0.5, 0.6, 0.5, 0.3);

        // Smoke puff at ground level
        world.spawnParticle(Particle.POOF, location.clone().add(0, 0.2, 0), 10, 0.3, 0.1, 0.3, 0.02);

        // Soul particles rising over time
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int tick = 0;
            final Location base = particleLoc.clone();

            @Override
            public void run() {
                if (tick >= 6) return; // 6 ticks * 3 = 18 ticks total (~0.9 seconds)

                Location rising = base.clone().add(0, tick * 0.3, 0);
                world.spawnParticle(Particle.SOUL, rising, 3, 0.15, 0.1, 0.15, 0.01);
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, rising, 2, 0.1, 0.05, 0.1, 0.005);
                tick++;
            }
        }, 5L, 3L);
    }

    /**
     * Spawn winner celebration - actual firework rockets + particle effects
     */
    public void spawnWinnerParticles(Player winner) {
        if (winner == null) return;

        World world = winner.getLocation().getWorld();
        if (world == null) return;

        // Spawn 5 firework rockets over 3 seconds around the winner
        for (int i = 0; i < 5; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!winner.isOnline()) return;
                Location loc = winner.getLocation().clone();

                // Random offset around the winner
                double offsetX = (Math.random() - 0.5) * 4;
                double offsetZ = (Math.random() - 0.5) * 4;
                loc.add(offsetX, 0, offsetZ);

                spawnCelebrationFirework(loc);
            }, i * 12L); // Every 12 ticks (0.6s apart)
        }

        // Continuous particle spiral around the winner for 4 seconds
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= 80 || !winner.isOnline()) return;

                Location loc = winner.getLocation().add(0, 0.5, 0);

                // Spiral particles rising
                double angle = tick * 0.3;
                double radius = 1.2;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = (tick % 40) * 0.05;

                Location spiral = loc.clone().add(x, y, z);
                world.spawnParticle(Particle.END_ROD, spiral, 2, 0, 0, 0, 0);
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 3, 0.5, 0.8, 0.5, 0.1);

                // Periodic golden bursts
                if (tick % 20 == 0) {
                    world.spawnParticle(Particle.FIREWORK, loc.add(0, 1, 0), 30, 1.5, 1.5, 1.5, 0.1);
                }

                tick += 2;
            }
        }, 0L, 2L);
    }

    /**
     * Spawn a single celebration firework at a location
     */
    private void spawnCelebrationFirework(Location location) {
        if (location == null || location.getWorld() == null) return;

        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        // Random color combinations
        Color[] colors = {Color.YELLOW, Color.ORANGE, Color.RED, Color.AQUA, Color.LIME, Color.FUCHSIA, Color.WHITE};
        Color primary = colors[(int) (Math.random() * colors.length)];
        Color fade = colors[(int) (Math.random() * colors.length)];

        // Random firework type
        FireworkEffect.Type[] types = {
            FireworkEffect.Type.BALL_LARGE, FireworkEffect.Type.STAR, FireworkEffect.Type.BURST
        };
        FireworkEffect.Type type = types[(int) (Math.random() * types.length)];

        FireworkEffect effect = FireworkEffect.builder()
                .with(type)
                .withColor(primary)
                .withFade(fade)
                .flicker(Math.random() > 0.5)
                .trail(true)
                .build();

        meta.addEffect(effect);
        meta.setPower(1); // Low power = detonates quickly
        firework.setFireworkMeta(meta);
    }

    /**
     * Spawn event start particles at location
     */
    public void spawnEventStartParticles(Location location) {
        if (location == null || location.getWorld() == null) return;

        World world = location.getWorld();
        world.spawnParticle(Particle.EXPLOSION, location, 3, 2, 1, 2, 0);
        world.spawnParticle(Particle.FLASH, location, 1, 0, 0, 0, 0);
    }

    // ==================== ACTION BAR ====================

    /**
     * Send action bar message
     */
    public void sendActionBar(Player player, String message) {
        // Action bar removed per request
    }

    /**
     * Send action bar to all players in set
     */
    public void sendActionBarToAll(Set<UUID> players, String message) {
        // Action bar removed per request
    }

    // ==================== COMBINED EFFECTS ====================

    /**
     * Full kill feedback (sound + title + particles)
     */
    public void onKill(Player killer, Player victim, int killCount) {
        // Killer feedback
        if (killer != null) {
            playKillSound(killer);
            sendKillTitle(killer, victim.getName(), killCount);
        }

        // Victim feedback
        if (victim != null) {
            playDeathSound(victim);
            sendDeathTitle(victim, killer != null ? killer.getName() : null);
            spawnKillParticles(victim.getLocation());
        }
    }

    /**
     * Full winner feedback (sound + title + particles + fireworks for everyone)
     */
    public void onWin(Player winner) {
        if (winner == null) return;

        playWinnerSound(winner);
        spawnWinnerParticles(winner);

        // All players hear the celebration
        playSoundAll(Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.0f);
        Bukkit.getScheduler().runTaskLater(plugin, () ->
            playSoundAll(Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.6f, 1.2f), 20L);
        Bukkit.getScheduler().runTaskLater(plugin, () ->
            playSoundAll(Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.6f, 0.9f), 40L);
    }

    /**
     * Full team winner feedback
     */
    public void onTeamWin(Set<UUID> teamMembers, int teamNumber, org.bukkit.ChatColor teamColor) {
        for (UUID uuid : teamMembers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                playWinnerSound(player);
                spawnWinnerParticles(player);
            }
        }

        playSoundAll(Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.0f);
    }

    /**
     * Spectator mode feedback
     */
    public void onBecomeSpectator(Player player) {
        if (player == null) return;

        sendSpectatorTitle(player);
        playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
    }
}
