package me.oblueberrey.meowMcEvents.utils;

import org.bukkit.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final String SMALL_CAPS = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";

    // Improved RGB Color Palette
    public static final String PRIMARY = "&#FFE566";      // Soft gold
    public static final String ACCENT = "&#FFB344";       // Warm orange (slightly adjusted)
    public static final String HIGHLIGHT = "&#FFD966";    // Light gold/orange
    public static final String SUCCESS_COLOR = "&#A3FF78"; // Soft lime green
    public static final String ERROR_COLOR = "&#FF6B6B";   // Soft coral red
    public static final String INFO_COLOR = "&#78E3FF";    // Soft sky blue
    public static final String WARN_COLOR = "&#FFB344";    // Warm orange
    public static final String TEXT = "&#D1D1D1";          // Light grey text
    public static final String SUBTLE = "&#7A7A7A";        // Medium grey
    public static final String WHITE = "&#FFFFFF";         // Pure white
    public static final String PINK = "&#FF9ED2";          // Soft pink
    public static final String GOLD = "&#FFD700";          // Gold
    public static final String DARK_GREY = "&#555555";     // Dark grey
    public static final String BORDER_COLOR = "&#444444";  // Very dark grey for borders

    // Common Prefixes
    public static final String PREFIX = colorize("&6MeowEvents &8» ");
    public static final String INFO = colorize(INFO_COLOR + "ℹ ");
    public static final String SUCCESS = colorize(SUCCESS_COLOR + "✔ ");
    public static final String ERROR = colorize(ERROR_COLOR + "✘ ");
    public static final String WARNING = colorize(WARN_COLOR + "⚠ ");

    /**
     * Colorize a message supporting both legacy & codes and &#FFFFFF hex codes
     */
    public static String colorize(String message) {
        if (message == null || message.isEmpty()) return "";

        // Handle Hex colors
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String color = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : color.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);

        // Handle legacy & codes
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    /**
     * Create a gradient effect between multiple hex colors
     */
    public static String gradient(String text, String... colors) {
        if (text == null || text.isEmpty()) return "";
        if (colors == null || colors.length == 0) return text;
        if (colors.length == 1) return colorize(colors[0] + text);

        StringBuilder builder = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (float) (length - 1);
            if (length == 1) ratio = 0;
            
            float segment = ratio * (colors.length - 1);
            int index = (int) Math.min(Math.floor(segment), colors.length - 2);
            float localRatio = segment - index;

            String color1 = colors[index].replace("#", "").replace("&", "");
            String color2 = colors[index + 1].replace("#", "").replace("&", "");

            int r1 = Integer.parseInt(color1.substring(0, 2), 16);
            int g1 = Integer.parseInt(color1.substring(2, 4), 16);
            int b1 = Integer.parseInt(color1.substring(4, 6), 16);

            int r2 = Integer.parseInt(color2.substring(0, 2), 16);
            int g2 = Integer.parseInt(color2.substring(2, 4), 16);
            int b2 = Integer.parseInt(color2.substring(4, 6), 16);

            int r = (int) (r1 + (r2 - r1) * localRatio);
            int g = (int) (g1 + (g2 - g1) * localRatio);
            int b = (int) (b1 + (b2 - b1) * localRatio);

            String hex = String.format("%02x%02x%02x", r, g, b);
            builder.append("&#").append(hex).append(text.charAt(i));
        }

        return colorize(builder.toString());
    }

    /**
     * Create a rainbow effect on text
     */
    public static String rainbow(String text) {
        return gradient(text, "#FF0000", "#FF7F00", "#FFFF00", "#00FF00", "#0000FF", "#4B0082", "#8B00FF");
    }

    /**
     * Center a message in Minecraft chat (assuming standard width)
     */
    public static String center(String message) {
        if (message == null || message.isEmpty()) return "";
        int messagePx = getStringWidth(message);
        int halfDefault = 154; // Half of Minecraft chat width (approx 310-320px)
        int compensated = halfDefault - (messagePx / 2);
        if (compensated <= 0) return message;

        StringBuilder sb = new StringBuilder();
        int spaceWidth = 4; // Space is 4px
        int spaces = 0;
        while (spaces < compensated) {
            sb.append(" ");
            spaces += spaceWidth;
        }
        return sb.toString() + message;
    }


    /**
     * Calculate the pixel width of a string in Minecraft chat
     */
    public static int getStringWidth(String message) {
        if (message == null || message.isEmpty()) return 0;
        String stripped = ChatColor.stripColor(colorize(message));
        int width = 0;
        for (char c : stripped.toCharArray()) {
            // Standard char widths for Minecraft
            if (c == 'i' || c == '!') width += 2;
            else if (c == 'l' || c == '.' || c == ',' || c == ':' || c == ';') width += 3;
            else if (c == 't' || c == ' ' || c == '[' || c == ']' || c == '(' || c == ')' || c == '{' || c == '}') width += 4;
            else if (c == 'f' || c == 'k' || c == '<' || c == '>') width += 5;
            else width += 6;
        }
        return width;
    }

    /**
     * Generate a strikethrough line of a specific pixel width
     */
    public static String getLine(int width, String colorCode) {
        // Space is 4px wide, plus 1px spacing
        // We use &m (strikethrough) on spaces to create a solid line
        StringBuilder sb = new StringBuilder(colorCode).append("&m");
        int currentWidth = 0;
        while (currentWidth < width) {
            sb.append(" ");
            currentWidth += 4;
        }
        return colorize(sb.toString());
    }

    /**
     * Capitalizes the first letter of each sentence in the string.
     */
    public static String capitalizeSentences(String message) {
        if (message == null || message.isEmpty()) return "";
        
        StringBuilder result = new StringBuilder(message.length());
        boolean capitalizeNext = true;
        boolean inColorCode = false;

        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);

            // Handle legacy color codes (e.g., &c, §c)
            if ((c == '&' || c == '§') && i + 1 < message.length()) {
                result.append(c);
                result.append(message.charAt(++i));
                continue;
            }
            
            // Handle hex color codes (e.g., &#RRGGBB)
            if (c == '&' && i + 1 < message.length() && message.charAt(i+1) == '#') {
                result.append("&#");
                i += 2;
                for (int j = 0; j < 6 && i < message.length(); j++, i++) {
                    result.append(message.charAt(i));
                }
                i--; // Adjust for outer loop increment
                continue;
            }

            if (capitalizeNext && Character.isLetter(c)) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }

            if (c == '.' || c == '!' || c == '?') {
                capitalizeNext = true;
            }
        }
        return result.toString();
    }

    /**
     * Converts text to small caps Unicode characters.
     */
    public static String toSmallCaps(String text) {
        if (text == null) return "";
        StringBuilder result = new StringBuilder();
        for (char c : text.toLowerCase().toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                result.append(SMALL_CAPS.charAt(c - 'a'));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Format a message with colorization and sentence capitalization.
     */
    public static String format(String message) {
        return colorize(capitalizeSentences(message));
    }

    /**
     * Sends a formatted message to a player/sender with the prefix.
     */
    public static void sendMessage(org.bukkit.command.CommandSender sender, String message) {
        sender.sendMessage(colorize(PREFIX + message));
    }

    public static void sendSuccess(org.bukkit.command.CommandSender sender, String message) {
        sender.sendMessage(colorize(PREFIX + SUCCESS + "§7" + message));
    }

    public static void sendError(org.bukkit.command.CommandSender sender, String message) {
        sender.sendMessage(colorize(PREFIX + ERROR + "§c" + message));
    }

    public static void sendInfo(org.bukkit.command.CommandSender sender, String message) {
        sender.sendMessage(colorize(PREFIX + INFO + "§7" + message));
    }
}
