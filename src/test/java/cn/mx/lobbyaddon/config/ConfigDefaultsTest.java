package cn.mx.lobbyaddon.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigDefaultsTest {

    @Test
    void distinguishesFileValuesFromInheritedDefaults() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("feature.enabled", true);
        YamlConfiguration current = new YamlConfiguration();
        current.setDefaults(defaults);

        assertFalse(ConfigDefaults.hasExplicitValue(current, "feature.enabled"));
        current.set("feature.enabled", false);
        assertTrue(ConfigDefaults.hasExplicitValue(current, "feature.enabled"));
    }
}
