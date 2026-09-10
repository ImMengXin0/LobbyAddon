package cn.mx.lobbyaddon.chat;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ChatColorParser {

    private static final Map<String, Character> LEGACY_COLORS;

    static {
        Map<String, Character> colors = new LinkedHashMap<>();
        colors.put("black", '0');
        colors.put("dark_blue", '1');
        colors.put("dark_green", '2');
        colors.put("dark_aqua", '3');
        colors.put("dark_red", '4');
        colors.put("dark_purple", '5');
        colors.put("gold", '6');
        colors.put("gray", '7');
        colors.put("grey", '7');
        colors.put("dark_gray", '8');
        colors.put("dark_grey", '8');
        colors.put("blue", '9');
        colors.put("green", 'a');
        colors.put("aqua", 'b');
        colors.put("cyan", 'b');
        colors.put("red", 'c');
        colors.put("light_purple", 'd');
        colors.put("pink", 'd');
        colors.put("yellow", 'e');
        colors.put("white", 'f');
        LEGACY_COLORS = Collections.unmodifiableMap(colors);
    }

    private ChatColorParser() {
    }

    public static Character parseLegacy(String input) {
        if (input == null) {
            return null;
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (normalized.length() == 2 && (normalized.charAt(0) == '&' || normalized.charAt(0) == '\u00a7')) {
            normalized = normalized.substring(1);
        }
        if (normalized.length() == 1 && "0123456789abcdef".indexOf(normalized.charAt(0)) >= 0) {
            return normalized.charAt(0);
        }
        return LEGACY_COLORS.get(normalized);
    }

    public static Integer parseHex(String input) {
        if (input == null) {
            return null;
        }
        String normalized = input.trim();
        if (normalized.startsWith("&#")) {
            normalized = normalized.substring(2);
        } else if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.matches("(?i)[0-9a-f]{6}")) {
            return null;
        }
        try {
            return Integer.parseInt(normalized, 16);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static Map<String, Character> legacyColors() {
        return LEGACY_COLORS;
    }
}
