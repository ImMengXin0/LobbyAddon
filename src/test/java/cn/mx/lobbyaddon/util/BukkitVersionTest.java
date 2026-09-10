package cn.mx.lobbyaddon.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BukkitVersionTest {

    @Test
    void enablesHexColorsOnlyOn116AndNewer() {
        assertFalse(BukkitVersion.supportsHexColors("1.8.8-R0.1-SNAPSHOT"));
        assertFalse(BukkitVersion.supportsHexColors("1.15.2-R0.1-SNAPSHOT"));
        assertTrue(BukkitVersion.supportsHexColors("1.16.5-R0.1-SNAPSHOT"));
        assertTrue(BukkitVersion.supportsHexColors("1.21.1-R0.1-SNAPSHOT"));
    }
}
