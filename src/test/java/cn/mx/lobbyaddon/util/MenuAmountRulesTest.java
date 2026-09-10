package cn.mx.lobbyaddon.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MenuAmountRulesTest {

    @Test
    void displaysZeroAsOneWithoutTreatingItAsFailure() {
        assertEquals(1, MenuAmountRules.toDisplayAmount(0, 64));
    }

    @Test
    void rejectsFailedOrNegativeValues() {
        assertEquals(-1, MenuAmountRules.toDisplayAmount(-1, 64));
    }

    @Test
    void usesMaterialStackLimit() {
        assertEquals(16, MenuAmountRules.toDisplayAmount(32, 16));
        assertEquals(64, MenuAmountRules.toDisplayAmount(80, 64));
    }
}
