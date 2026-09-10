package cn.mx.lobbyaddon.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Shared text rules for profile values and comparison keys. */
public final class ProfileTextRules {

    private ProfileTextRules() {
    }

    public static String normalizeForComparison(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC);
        StringBuilder result = new StringBuilder(normalized.length());
        for (int offset = 0; offset < normalized.length();) {
            int codePoint = normalized.codePointAt(offset);
            if (Character.getType(codePoint) != Character.FORMAT) {
                result.appendCodePoint(codePoint);
            }
            offset += Character.charCount(codePoint);
        }
        return result.toString().toLowerCase(Locale.ROOT);
    }

    public static boolean containsInvisibleCharacter(String value) {
        if (value == null) {
            return false;
        }
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            if (Character.getType(codePoint) == Character.FORMAT) {
                return true;
            }
            offset += Character.charCount(codePoint);
        }
        return false;
    }

    public static boolean usesColorCode(String value, char configuredCode) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String introducers = Pattern.quote(String.valueOf(configuredCode) + "\u00a7");
        return Pattern.compile("(?i)[" + introducers + "](?:[0-9a-fk-or]|#[0-9a-f]{6})")
                .matcher(value).find();
    }

    public static boolean exceedsRawLength(String value, int maximum) {
        return value != null && value.codePointCount(0, value.length()) > Math.max(1, maximum);
    }
}
