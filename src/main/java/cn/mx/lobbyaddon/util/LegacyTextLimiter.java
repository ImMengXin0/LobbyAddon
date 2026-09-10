package cn.mx.lobbyaddon.util;

public final class LegacyTextLimiter {

    private LegacyTextLimiter() {
    }

    public static String limitSerialized(String value, int maximumLength) {
        if (value == null || value.isEmpty() || maximumLength <= 0) {
            return "";
        }
        if (value.length() <= maximumLength && !value.endsWith("\u00a7")) {
            return value;
        }
        StringBuilder result = new StringBuilder(maximumLength);
        int index = 0;
        while (index < value.length() && result.length() < maximumLength) {
            if (value.charAt(index) == '\u00a7' && index + 1 >= value.length()) {
                break;
            }
            int tokenLength = formattingTokenLength(value, index);
            if (tokenLength > 0) {
                if (result.length() + tokenLength <= maximumLength) {
                    result.append(value, index, index + tokenLength);
                }
                index += tokenLength;
                continue;
            }
            int codePoint = value.codePointAt(index);
            int charCount = Character.charCount(codePoint);
            if (result.length() + charCount > maximumLength) {
                break;
            }
            result.appendCodePoint(codePoint);
            index += charCount;
        }
        return result.toString();
    }

    private static int formattingTokenLength(String value, int index) {
        if (value.charAt(index) != '\u00a7' || index + 1 >= value.length()) {
            return 0;
        }
        if ((value.charAt(index + 1) == 'x' || value.charAt(index + 1) == 'X')
                && isCompleteHexSequence(value, index)) {
            return 14;
        }
        return 2;
    }

    private static boolean isCompleteHexSequence(String value, int index) {
        if (index + 14 > value.length()) {
            return false;
        }
        for (int offset = 2; offset < 14; offset += 2) {
            if (value.charAt(index + offset) != '\u00a7'
                    || Character.digit(value.charAt(index + offset + 1), 16) < 0) {
                return false;
            }
        }
        return true;
    }
}
