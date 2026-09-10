package cn.mx.lobbyaddon.config;

public final class ConfigSanity {

    private ConfigSanity() {
    }

    public static boolean isStorageType(String value) {
        return value != null && ("yaml".equalsIgnoreCase(value) || "mysql".equalsIgnoreCase(value));
    }

    public static boolean isMysqlTable(String value) {
        return value != null && value.matches("[A-Za-z0-9_]+");
    }

    public static boolean isLengthRange(int minimum, int maximum) {
        return minimum >= 0 && maximum >= minimum;
    }

    public static boolean isInventorySize(int size) {
        return size >= 9 && size <= 54 && size % 9 == 0;
    }

    public static boolean isNonNegative(long value) {
        return value >= 0L;
    }
}
