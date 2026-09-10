package cn.mx.lobbyaddon.menu;

import java.util.Locale;

public final class PersonalMenuInput {

    private PersonalMenuInput() {
    }

    public static boolean isCancel(String input) {
        if (input == null) {
            return false;
        }
        String normalized = input.trim();
        return "取消".equals(normalized) || "cancel".equalsIgnoreCase(normalized);
    }

    public static boolean isSafe(String input) {
        if (input == null || input.trim().isEmpty() || input.trim().startsWith("/")) {
            return false;
        }
        for (int index = 0; index < input.length(); index++) {
            if (Character.isISOControl(input.charAt(index)) || input.charAt(index) == '\u00a7') {
                return false;
            }
        }
        return true;
    }

    public static String profileCommand(String command, long durationMillis, String value) {
        if (durationMillis <= 0L) {
            return command + " " + value;
        }
        return command + " temp " + durationToken(durationMillis) + " " + value;
    }

    public static String hexCommand(String input) {
        String color = normalizedHex(input);
        return color == null ? null : "chatcolor hex " + color;
    }

    public static String gradientCommand(String input) {
        if (input == null) {
            return null;
        }
        String[] colors = input.trim().split("\\s+");
        if (colors.length != 2) {
            return null;
        }
        String start = normalizedHex(colors[0]);
        String end = normalizedHex(colors[1]);
        return start == null || end == null ? null : "chatcolor gradient " + start + " " + end;
    }

    private static String normalizedHex(String input) {
        if (input == null) {
            return null;
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT);
        return normalized.matches("#[0-9A-F]{6}") ? normalized : null;
    }

    private static String durationToken(long durationMillis) {
        if (durationMillis == 30L * 60L * 1000L) {
            return "30m";
        }
        if (durationMillis == 60L * 60L * 1000L) {
            return "1h";
        }
        if (durationMillis == 24L * 60L * 60L * 1000L) {
            return "1d";
        }
        return Math.max(1L, durationMillis / 1000L) + "s";
    }
}
