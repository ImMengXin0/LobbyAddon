package cn.mx.lobbyaddon.util;

import java.util.Locale;

public final class DurationParser {

    private DurationParser() {
    }

    public static long parseMillis(String input) {
        if (input == null) {
            return -1L;
        }
        String value = input.trim().toLowerCase(Locale.ROOT);
        if (value.length() < 2) {
            return -1L;
        }
        char unit = value.charAt(value.length() - 1);
        long multiplier;
        if (unit == 's') {
            multiplier = 1000L;
        } else if (unit == 'm') {
            multiplier = 60_000L;
        } else if (unit == 'h') {
            multiplier = 3_600_000L;
        } else if (unit == 'd') {
            multiplier = 86_400_000L;
        } else {
            return -1L;
        }
        try {
            long amount = Long.parseLong(value.substring(0, value.length() - 1));
            return amount > 0L && amount <= Long.MAX_VALUE / multiplier ? amount * multiplier : -1L;
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }
}
