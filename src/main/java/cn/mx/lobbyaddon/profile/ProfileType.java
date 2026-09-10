package cn.mx.lobbyaddon.profile;

import java.util.Locale;

public enum ProfileType {
    NICK("nick"),
    PREFIX("prefix"),
    SUFFIX("suffix");

    private final String configKey;

    ProfileType(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }

    public static ProfileType fromInput(String input) {
        if (input == null) {
            return null;
        }
        String value = input.toLowerCase(Locale.ROOT);
        if ("nick".equals(value) || "nickname".equals(value)) {
            return NICK;
        }
        if ("prefix".equals(value) || "custom".equals(value) || "title".equals(value)) {
            return PREFIX;
        }
        if ("suffix".equals(value)) {
            return SUFFIX;
        }
        return null;
    }
}
