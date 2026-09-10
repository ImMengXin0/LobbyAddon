package cn.mx.lobbyaddon.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerNameResolverTest {

    @Test
    void usesLegacyBukkitServerNameWhenAvailable() {
        assertEquals("legacy-lobby",
                ServerNameResolver.resolve(new LegacyServer(), "configured-lobby"));
    }

    @Test
    void fallsBackToConfiguredNameWhenModernServerRemovedMethod() {
        assertEquals("modern-lobby",
                ServerNameResolver.resolve(new ModernServer(), " modern-lobby "));
    }

    @Test
    void usesSafeDefaultForBlankConfiguredName() {
        assertEquals("lobby", ServerNameResolver.resolve(new ModernServer(), "  "));
        assertEquals("lobby", ServerNameResolver.resolve(null, null));
    }

    public static final class LegacyServer {
        public String getServerName() {
            return " legacy-lobby ";
        }
    }

    public static final class ModernServer {
        public String getName() {
            return "ASPaper";
        }
    }
}
