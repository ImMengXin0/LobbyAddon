package cn.mx.lobbyaddon.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileTextRulesTest {

    @Test
    void normalizesFullWidthTextForComparison() {
        assertEquals("admin", ProfileTextRules.normalizeForComparison("ＡＤＭＩＮ"));
    }

    @Test
    void rejectsInvisibleFormattingCharacters() {
        assertTrue(ProfileTextRules.containsInvisibleCharacter("Ad\u200Bmin"));
        assertFalse(ProfileTextRules.containsInvisibleCharacter("正常称号"));
    }

    @Test
    void recognizesOnlyActualColorCodes() {
        assertTrue(ProfileTextRules.usesColorCode("&a绿色", '&'));
        assertTrue(ProfileTextRules.usesColorCode("&#12ABef彩色", '&'));
        assertFalse(ProfileTextRules.usesColorCode("A&ZB", '&'));
    }

    @Test
    void enforcesRawLengthSeparatelyFromVisibleLength() {
        assertTrue(ProfileTextRules.exceedsRawLength("&a&a&aName", 8));
        assertFalse(ProfileTextRules.exceedsRawLength("&aName", 8));
    }
}
