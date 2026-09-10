package cn.mx.lobbyaddon.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSanityTest {

    @Test
    void acceptsSupportedStorageTypes() {
        assertTrue(ConfigSanity.isStorageType("yaml"));
        assertTrue(ConfigSanity.isStorageType("MYSQL"));
        assertFalse(ConfigSanity.isStorageType("sqlite"));
        assertFalse(ConfigSanity.isStorageType(null));
    }

    @Test
    void acceptsSafeMysqlTableNames() {
        assertTrue(ConfigSanity.isMysqlTable("nick_data_2"));
        assertFalse(ConfigSanity.isMysqlTable("nick-data"));
        assertFalse(ConfigSanity.isMysqlTable(""));
        assertFalse(ConfigSanity.isMysqlTable(null));
    }

    @Test
    void validatesLengthRanges() {
        assertTrue(ConfigSanity.isLengthRange(1, 16));
        assertFalse(ConfigSanity.isLengthRange(-1, 16));
        assertFalse(ConfigSanity.isLengthRange(10, 5));
    }

    @Test
    void validatesInventorySizes() {
        assertTrue(ConfigSanity.isInventorySize(9));
        assertTrue(ConfigSanity.isInventorySize(54));
        assertFalse(ConfigSanity.isInventorySize(10));
        assertFalse(ConfigSanity.isInventorySize(63));
    }

    @Test
    void validatesNonNegativeIntervals() {
        assertTrue(ConfigSanity.isNonNegative(0));
        assertFalse(ConfigSanity.isNonNegative(-1));
    }
}
