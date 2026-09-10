package cn.mx.lobbyaddon.util;

import java.util.Locale;

public final class MenuPlaceholder {

    private static final String LOBBY_PREFIX = "%lobbyaddon_online_";
    private static final String NICK_PREFIX = "%nick_online_";

    private MenuPlaceholder() {
    }

    public static String onlineServer(String raw) {
        if (raw == null) {
            return null;
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        String prefix = lower.startsWith(LOBBY_PREFIX) ? LOBBY_PREFIX
                : lower.startsWith(NICK_PREFIX) ? NICK_PREFIX : null;
        if (prefix == null || !lower.endsWith("%")) {
            return null;
        }
        String server = lower.substring(prefix.length(), lower.length() - 1);
        return server.isEmpty() ? null : server;
    }
}
