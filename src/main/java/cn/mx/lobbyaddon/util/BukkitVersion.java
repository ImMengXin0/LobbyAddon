package cn.mx.lobbyaddon.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BukkitVersion {

    private static final Pattern VERSION = Pattern.compile("(?:^|[^0-9])1\\.(\\d+)(?:\\.|-|$)");

    private BukkitVersion() {
    }

    public static boolean supportsHexColors(String bukkitVersion) {
        if (bukkitVersion == null) {
            return false;
        }
        Matcher matcher = VERSION.matcher(bukkitVersion);
        if (!matcher.find()) {
            return false;
        }
        try {
            return Integer.parseInt(matcher.group(1)) >= 16;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
