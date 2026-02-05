package me.oblueberrey.meowMcEvents.managers;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.utils.ConfigManager;
import me.oblueberrey.meowMcEvents.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles stylized kill feed messages with weapon icons, randomized messages,
 * distance tracking, and situational announcements.
 */
public class KillFeedManager {

    private final MeowMCEvents plugin;
    private final Random random = new Random();

    // Track kills for special messages
    private final Map<UUID, UUID> lastKilledBy = new ConcurrentHashMap<>(); // victim -> killer
    private boolean firstBloodAnnounced = false;

    // Improved RGB Colors
    private static final String GOLD = "&#FFE566";
    private static final String ORANGE = "&#FF9944";
    private static final String GREY = "&#AAAAAA";
    private static final String RED = "&#FF5555";
    private static final String GREEN = "&#55FF55";
    private static final String AQUA = "&#55FFFF";
    private static final String PINK = "&#FF7EB3";
    private static final String WHITE = "&#FFFFFF";
    private static final String DARK_GREY = "&#666666";
    private static final String PURPLE = "&#AA55FF";
    private static final String LIGHT_RED = "&#FF8888";

    // Weapon icons (Unicode)
    private static final String SWORD_ICON = "\u2694";
    private static final String BOW_ICON = "\u27B3";
    private static final String AXE_ICON = "\u2692";
    private static final String TRIDENT_ICON = "\u2191";
    private static final String FIST_ICON = "\u270A";
    private static final String SKULL_ICON = "\u2620";
    private static final String FIRE_ICON = "\u2739";
    private static final String MAGIC_ICON = "\u2728";
    private static final String EXPLOSION_ICON = "\u2600";
    private static final String FALL_ICON = "\u2193";
    private static final String VOID_ICON = "\u2205";
    private static final String DROWN_ICON = "\u224B";
    private static final String BORDER_ICON = "\u26A0";
    private static final String STAR_ICON = "\u2605";
    private static final String CROSSHAIR_ICON = "\u25CE";

    // Streak messages
    private static final String[] STREAK_TITLES = {
        "", "", "",
        "TRIPLE KILL",
        "QUAD KILL",
        "PENTA KILL",
        "LEGENDARY",
        "GODLIKE",
        "UNSTOPPABLE",
        "RAMPAGE",
        "DOMINATING"
    };

    // ==================== Randomized Kill Messages ====================

    private static final String[][] SWORD_MESSAGES = {
        {"%killer%", "sliced", "%victim%"},
        {"%killer%", "cut down", "%victim%"},
        {"%killer%", "slashed", "%victim%"},
        {"%killer%", "stabbed", "%victim%"},
        {"%killer%", "impaled", "%victim%"},
    };

    private static final String[][] AXE_MESSAGES = {
        {"%killer%", "chopped", "%victim%"},
        {"%killer%", "cleaved", "%victim%"},
        {"%killer%", "axed", "%victim%"},
        {"%killer%", "split", "%victim%", "in two"},
        {"%killer%", "hacked", "%victim%", "apart"},
    };

    private static final String[][] BOW_MESSAGES = {
        {"%killer%", "shot", "%victim%"},
        {"%killer%", "sniped", "%victim%"},
        {"%killer%", "pierced", "%victim%"},
        {"%killer%", "pinned", "%victim%"},
        {"%killer%", "arrowed", "%victim%"},
    };

    private static final String[][] TRIDENT_MESSAGES = {
        {"%killer%", "impaled", "%victim%", "with a trident"},
        {"%killer%", "skewered", "%victim%"},
        {"%killer%", "speared", "%victim%"},
    };

    private static final String[][] FIST_MESSAGES = {
        {"%killer%", "punched", "%victim%", "to death"},
        {"%killer%", "beat", "%victim%", "with bare fists"},
        {"%killer%", "pummeled", "%victim%"},
        {"%killer%", "knocked out", "%victim%"},
    };

    private static final String[][] GENERIC_MESSAGES = {
        {"%killer%", "killed", "%victim%"},
        {"%killer%", "eliminated", "%victim%"},
        {"%killer%", "took out", "%victim%"},
        {"%killer%", "finished", "%victim%"},
    };

    // Environmental death messages (no killer)
    private static final String[][] FALL_MESSAGES = {
        {"%victim%", "fell to their death"},
        {"%victim%", "forgot they can't fly"},
        {"%victim%", "hit the ground too hard"},
        {"%victim%", "experienced gravity"},
        {"%victim%", "made a leap of faith... and failed"},
    };

    private static final String[][] VOID_MESSAGES = {
        {"%victim%", "fell into the void"},
        {"%victim%", "was consumed by the void"},
        {"%victim%", "discovered the abyss"},
    };

    private static final String[][] FIRE_MESSAGES = {
        {"%victim%", "burned to a crisp"},
        {"%victim%", "couldn't handle the heat"},
        {"%victim%", "was roasted alive"},
        {"%victim%", "went up in flames"},
    };

    private static final String[][] LAVA_MESSAGES = {
        {"%victim%", "tried to swim in lava"},
        {"%victim%", "took a lava bath"},
        {"%victim%", "melted"},
    };

    private static final String[][] DROWN_MESSAGES = {
        {"%victim%", "drowned"},
        {"%victim%", "forgot to breathe"},
        {"%victim%", "slept with the fishes"},
    };

    private static final String[][] EXPLOSION_MESSAGES = {
        {"%victim%", "blew up"},
        {"%victim%", "was blown to bits"},
        {"%victim%", "exploded"},
    };

    private static final String[][] GENERIC_DEATH_MESSAGES = {
        {"%victim%", "died"},
        {"%victim%", "was eliminated"},
        {"%victim%", "perished"},
        {"%victim%", "met their end"},
    };

    public KillFeedManager(MeowMCEvents plugin) {
        this.plugin = plugin;
    }

    /**
     * Reset state for a new event
     */
    public void reset() {
        lastKilledBy.clear();
        firstBloodAnnounced = false;
    }

    /**
     * Broadcast a stylized kill message with randomized text
     */
    public void broadcastKill(Player killer, Player victim, int killStreak) {
        TeamManager teamManager = plugin.getTeamManager();

        ChatColor killerColor = getPlayerColor(killer, teamManager);
        ChatColor victimColor = getPlayerColor(victim, teamManager);

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        String weaponIcon = getWeaponIcon(weapon);
        String weaponName = getWeaponName(weapon);
        String weaponCategory = getWeaponCategory(weapon);

        // Calculate distance
        double distance = killer.getLocation().distance(victim.getLocation());

        // Check for special situations
        boolean isFirstBlood = !firstBloodAnnounced;
        boolean isRevenge = isRevenge(killer, victim);
        boolean isLongShot = distance >= 30 && isRangedWeapon(weapon);
        boolean isOneShot = victim.getMaxHealth() == victim.getHealth(); // Full health kill (unlikely but possible)

        // Mark first blood
        if (isFirstBlood) {
            firstBloodAnnounced = true;
        }

        // Track for revenge
        lastKilledBy.put(victim.getUniqueId(), killer.getUniqueId());

        // Build and broadcast the main kill message
        String killMessage = buildKillMessage(killer, victim, killerColor, victimColor,
                weaponIcon, weaponName, weaponCategory, distance);
        Bukkit.broadcastMessage(killMessage);

        // Special announcements
        if (isFirstBlood) {
            broadcastSpecialMessage(STAR_ICON, "FIRST BLOOD", killerColor + killer.getName(),
                    "drew first blood!", RED);
        }

        if (isRevenge) {
            broadcastSpecialMessage(SWORD_ICON, "REVENGE", killerColor + killer.getName(),
                    "got revenge on " + victimColor + victim.getName() + GREY + "!", ORANGE);
        }

        if (isLongShot) {
            int dist = (int) distance;
            broadcastSpecialMessage(CROSSHAIR_ICON, "LONG SHOT", killerColor + killer.getName(),
                    "sniped from " + WHITE + dist + " blocks" + GREY + "!", AQUA);
        }

        // Streak announcement
        if (killStreak >= 3) {
            broadcastStreakMessage(killer, killerColor, killStreak);
        }
    }

    /**
     * Broadcast an environmental death message (no killer)
     */
    public void broadcastEnvironmentalDeath(Player victim, EntityDamageEvent.DamageCause cause) {
        TeamManager teamManager = plugin.getTeamManager();
        ChatColor victimColor = getPlayerColor(victim, teamManager);

        String[][] messagePool = getEnvironmentalMessages(cause);
        String[] chosen = messagePool[random.nextInt(messagePool.length)];

        String deathIcon = getDeathCauseIcon(cause);

        // Build message
        StringBuilder msg = new StringBuilder();
        msg.append(ConfigManager.colorize(DARK_GREY + deathIcon + " "));

        for (String part : chosen) {
            if (part.equals("%victim%")) {
                msg.append(victimColor).append(victim.getName());
            } else {
                msg.append(ConfigManager.colorize(GREY + " " + part));
            }
        }

        Bukkit.broadcastMessage(msg.toString());
    }

    /**
     * Broadcast a border damage death
     */
    public void broadcastBorderDeath(Player victim) {
        TeamManager teamManager = plugin.getTeamManager();
        ChatColor victimColor = getPlayerColor(victim, teamManager);

        String[] messages = {
            " was consumed by the border",
            " stayed in the danger zone too long",
            " was punished by the arena",
            " couldn't escape the border",
        };

        String chosen = messages[random.nextInt(messages.length)];
        String message = ConfigManager.colorize(
                DARK_GREY + BORDER_ICON + " " + victimColor + victim.getName() + GREY + chosen);
        Bukkit.broadcastMessage(message);
    }

    /**
     * Build the kill message with randomized verb
     */
    private String buildKillMessage(Player killer, Player victim, ChatColor killerColor,
                                     ChatColor victimColor, String weaponIcon, String weaponName,
                                     String weaponCategory, double distance) {

        String[][] messagePool = getKillMessages(weaponCategory);
        String[] chosen = messagePool[random.nextInt(messagePool.length)];

        StringBuilder msg = new StringBuilder();
        msg.append(ConfigManager.colorize(DARK_GREY + SKULL_ICON + " "));

        for (int i = 0; i < chosen.length; i++) {
            String part = chosen[i];
            if (part.equals("%killer%")) {
                msg.append(killerColor).append(killer.getName());
            } else if (part.equals("%victim%")) {
                msg.append(victimColor).append(victim.getName());
            } else {
                msg.append(ConfigManager.colorize(GREY + " " + part));
            }
            if (i < chosen.length - 1 && !chosen[i + 1].equals("%victim%") && !part.equals("%killer%")) {
                // Don't double-space
            }
        }

        // Append weapon tag
        msg.append(ConfigManager.colorize(" " + DARK_GREY + "[" + ORANGE + weaponIcon + " " + GOLD + weaponName + DARK_GREY + "]"));

        // Append distance for ranged kills
        if (distance >= 15 && isRangedCategory(weaponCategory)) {
            msg.append(ConfigManager.colorize(" " + AQUA + (int) distance + "m"));
        }

        return msg.toString();
    }

    /**
     * Broadcast a special situational message
     */
    private void broadcastSpecialMessage(String icon, String title, String playerName,
                                          String description, String titleColor) {
        String message = ConfigManager.colorize(
                DARK_GREY + icon + " " + titleColor + "&l" + title + " " +
                        GREY + "- " + playerName + " " + GREY + description);
        Bukkit.broadcastMessage(message);
    }

    /**
     * Broadcast a kill streak message
     */
    private void broadcastStreakMessage(Player killer, ChatColor killerColor, int streak) {
        String streakTitle = streak >= STREAK_TITLES.length ?
            STREAK_TITLES[STREAK_TITLES.length - 1] : STREAK_TITLES[streak];
        if (streakTitle.isEmpty()) return;

        String streakText = MessageUtils.colorize("&c&l" + streakTitle);
        String message = ConfigManager.colorize(
            DARK_GREY + STAR_ICON + " " + streakText + " " + GREY + "- " +
            killerColor + killer.getName() + GREY + " (" + RED + streak + " kills" + GREY + ")"
        );
        Bukkit.broadcastMessage(message);

        if (streak >= 5) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(),
                    org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
            }
        }
    }

    // ==================== Helper Methods ====================

    private boolean isRevenge(Player killer, Player victim) {
        UUID lastKiller = lastKilledBy.get(killer.getUniqueId());
        return lastKiller != null && lastKiller.equals(victim.getUniqueId());
    }

    private boolean isRangedWeapon(ItemStack item) {
        if (item == null) return false;
        String name = item.getType().name().toLowerCase();
        return name.contains("bow") || name.contains("crossbow") || name.contains("trident");
    }

    private boolean isRangedCategory(String category) {
        return category.equals("bow") || category.equals("trident");
    }

    private String getWeaponCategory(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return "fist";
        String typeName = item.getType().name().toLowerCase();
        if (typeName.contains("sword")) return "sword";
        if (typeName.contains("axe") && !typeName.contains("pickaxe")) return "axe";
        if (typeName.contains("bow") || typeName.contains("crossbow")) return "bow";
        if (typeName.contains("trident")) return "trident";
        return "generic";
    }

    private String[][] getKillMessages(String category) {
        switch (category) {
            case "sword": return SWORD_MESSAGES;
            case "axe": return AXE_MESSAGES;
            case "bow": return BOW_MESSAGES;
            case "trident": return TRIDENT_MESSAGES;
            case "fist": return FIST_MESSAGES;
            default: return GENERIC_MESSAGES;
        }
    }

    private String[][] getEnvironmentalMessages(EntityDamageEvent.DamageCause cause) {
        if (cause == null) return GENERIC_DEATH_MESSAGES;
        switch (cause) {
            case FALL: return FALL_MESSAGES;
            case VOID: return VOID_MESSAGES;
            case DROWNING: return DROWN_MESSAGES;
            case FIRE:
            case FIRE_TICK: return FIRE_MESSAGES;
            case LAVA: return LAVA_MESSAGES;
            case ENTITY_EXPLOSION:
            case BLOCK_EXPLOSION: return EXPLOSION_MESSAGES;
            default: return GENERIC_DEATH_MESSAGES;
        }
    }

    private String getDeathCauseIcon(EntityDamageEvent.DamageCause cause) {
        if (cause == null) return SKULL_ICON;
        switch (cause) {
            case FALL: return FALL_ICON;
            case VOID: return VOID_ICON;
            case DROWNING: return DROWN_ICON;
            case FIRE:
            case FIRE_TICK:
            case LAVA: return FIRE_ICON;
            case ENTITY_EXPLOSION:
            case BLOCK_EXPLOSION: return EXPLOSION_ICON;
            case MAGIC:
            case WITHER:
            case POISON: return MAGIC_ICON;
            default: return SKULL_ICON;
        }
    }

    private ChatColor getPlayerColor(Player player, TeamManager teamManager) {
        if (teamManager == null) return ChatColor.WHITE;
        int team = teamManager.getTeam(player);
        if (team != -1) return teamManager.getTeamColor(team);
        return ChatColor.WHITE;
    }

    private String getWeaponIcon(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return FIST_ICON;
        String typeName = item.getType().name().toLowerCase();
        if (typeName.contains("sword")) return SWORD_ICON;
        if (typeName.contains("axe") && !typeName.contains("pickaxe")) return AXE_ICON;
        if (typeName.contains("bow") || typeName.contains("crossbow")) return BOW_ICON;
        if (typeName.contains("trident")) return TRIDENT_ICON;
        if (typeName.contains("end_crystal") || typeName.contains("totem")) return MAGIC_ICON;
        if (typeName.contains("tnt") || typeName.contains("firework")) return EXPLOSION_ICON;
        if (typeName.contains("flint") || typeName.contains("fire") || typeName.contains("lava")) return FIRE_ICON;
        return SWORD_ICON;
    }

    private String getWeaponName(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return "Fists";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return ChatColor.stripColor(item.getItemMeta().getDisplayName());
        }
        String name = item.getType().name().toLowerCase()
            .replace("_", " ")
            .replace("netherite", "Neth.")
            .replace("diamond", "Dia.")
            .replace("golden", "Gold")
            .replace("iron", "Iron")
            .replace("stone", "Stone")
            .replace("wooden", "Wood");

        StringBuilder formatted = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (c == ' ') {
                formatted.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                formatted.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                formatted.append(c);
            }
        }
        return formatted.toString();
    }
}
