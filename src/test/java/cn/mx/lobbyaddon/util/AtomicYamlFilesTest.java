package cn.mx.lobbyaddon.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AtomicYamlFilesTest {

    @TempDir
    Path directory;

    @Test
    void replacesPreviousYamlWithoutLeavingTemporaryFile() throws Exception {
        File target = directory.resolve("profiles.yml").toFile();
        YamlConfiguration first = new YamlConfiguration();
        first.set("value", "old");
        AtomicYamlFiles.save(first, target);

        YamlConfiguration second = new YamlConfiguration();
        second.set("value", "new");
        AtomicYamlFiles.save(second, target);

        assertEquals("new", YamlConfiguration.loadConfiguration(target).getString("value"));
        assertFalse(directory.resolve("profiles.yml.tmp").toFile().exists());
    }
}
