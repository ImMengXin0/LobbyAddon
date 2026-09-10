package cn.mx.lobbyaddon.util;

import java.lang.reflect.Method;

public final class ServerNameResolver {

    private static final String DEFAULT_SERVER_NAME = "lobby";

    private ServerNameResolver() {
    }

    public static String resolve(Object server, String configuredName) {
        String legacyName = invokeLegacyServerName(server);
        if (!legacyName.isEmpty()) {
            return legacyName;
        }
        String configured = trim(configuredName);
        return configured.isEmpty() ? DEFAULT_SERVER_NAME : configured;
    }

    private static String invokeLegacyServerName(Object server) {
        if (server == null) {
            return "";
        }
        try {
            Method method = server.getClass().getMethod("getServerName");
            Object value = method.invoke(server);
            return trim(value == null ? null : String.valueOf(value));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return "";
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
