package cn.mx.lobbyaddon.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LegacyTextLimiterTest {

    @Test
    void neverLeavesATrailingColorIntroducer() {
        String result = LegacyTextLimiter.limitSerialized("123456789012345\u00a7aX", 16);

        assertEquals("123456789012345X", result);
        assertFalse(result.endsWith("\u00a7"));
    }

    @Test
    void removesTrailingIntroducerEvenWhenInputAlreadyFits() {
        assertEquals("Name", LegacyTextLimiter.limitSerialized("Name\u00a7", 16));
    }

    @Test
    void keepsCompleteHexSequenceWhenItFits() {
        assertEquals("\u00a7x\u00a71\u00a72\u00a73\u00a74\u00a75\u00a76AB",
                LegacyTextLimiter.limitSerialized("\u00a7x\u00a71\u00a72\u00a73\u00a74\u00a75\u00a76ABC", 16));
    }

    @Test
    void omitsHexSequenceInsteadOfCuttingIt() {
        assertEquals("1234Name", LegacyTextLimiter.limitSerialized(
                "1234\u00a7x\u00a71\u00a72\u00a73\u00a74\u00a75\u00a76Name", 16));
    }
}
