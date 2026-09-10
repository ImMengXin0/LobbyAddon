package cn.mx.lobbyaddon.chat;

import java.util.Locale;

public enum ChatFormat {
    BOLD('l', "bold"),
    ITALIC('o', "italic"),
    UNDERLINE('n', "underline"),
    STRIKETHROUGH('m', "strikethrough"),
    OBFUSCATED('k', "obfuscated");

    private final char code;
    private final String key;

    ChatFormat(char code, String key) {
        this.code = code;
        this.key = key;
    }

    public char getCode() {
        return code;
    }

    public String getKey() {
        return key;
    }

    public static ChatFormat fromInput(String input) {
        if (input == null) {
            return null;
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if ("bold".equals(normalized) || "b".equals(normalized)) {
            return BOLD;
        }
        if ("italic".equals(normalized) || "italics".equals(normalized) || "i".equals(normalized)) {
            return ITALIC;
        }
        if ("underline".equals(normalized) || "underlined".equals(normalized)
                || "u".equals(normalized)) {
            return UNDERLINE;
        }
        if ("strikethrough".equals(normalized) || "strike".equals(normalized)
                || "s".equals(normalized)) {
            return STRIKETHROUGH;
        }
        if ("obfuscated".equals(normalized) || "magic".equals(normalized)
                || "random".equals(normalized) || "k".equals(normalized)) {
            return OBFUSCATED;
        }
        return null;
    }
}
