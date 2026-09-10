package cn.mx.lobbyaddon.util;

public final class MenuAmountRules {

    private MenuAmountRules() {
    }

    public static int toDisplayAmount(int value, int maximumStackSize) {
        if (value < 0) {
            return -1;
        }
        int maximum = Math.max(1, maximumStackSize);
        return Math.min(Math.max(1, value), maximum);
    }
}
