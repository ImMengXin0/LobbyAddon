package cn.mx.lobbyaddon.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DurationParserTest {

    @Test
    void parsesSupportedUnits() {
        assertEquals(30_000L, DurationParser.parseMillis("30s"));
        assertEquals(3_600_000L, DurationParser.parseMillis("1h"));
        assertEquals(604_800_000L, DurationParser.parseMillis("7d"));
    }

    @Test
    void rejectsInvalidAndOverflowingValues() {
        assertEquals(-1L, DurationParser.parseMillis("0m"));
        assertEquals(-1L, DurationParser.parseMillis("1w"));
        assertEquals(-1L, DurationParser.parseMillis("999999999999999999999d"));
    }
}
