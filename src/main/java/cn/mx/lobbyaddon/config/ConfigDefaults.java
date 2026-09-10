package cn.mx.lobbyaddon.config;

import org.bukkit.configuration.ConfigurationSection;

public final class ConfigDefaults {

    private ConfigDefaults() {
    }

    public static boolean hasExplicitValue(ConfigurationSection section, String path) {
        return section != null && path != null && section.getValues(true).containsKey(path);
    }
}
