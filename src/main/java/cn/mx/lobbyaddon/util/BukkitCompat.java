package cn.mx.lobbyaddon.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Bridges Bukkit's 1.8 array and 1.9+ collection online-player APIs. */
public final class BukkitCompat {

    private static final Method ONLINE_PLAYERS_METHOD = findOnlinePlayersMethod();

    private BukkitCompat() {
    }

    public static Collection<Player> getOnlinePlayers() {
        if (ONLINE_PLAYERS_METHOD == null) {
            return Collections.emptyList();
        }
        try {
            Object result = ONLINE_PLAYERS_METHOD.invoke(null);
            if (result instanceof Player[]) {
                return Arrays.asList((Player[]) result);
            }
            if (result instanceof Collection<?>) {
                List<Player> players = new ArrayList<>();
                for (Object value : (Collection<?>) result) {
                    if (value instanceof Player) {
                        players.add((Player) value);
                    }
                }
                return players;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return Collections.emptyList();
    }

    public static int getOnlinePlayerCount() {
        return getOnlinePlayers().size();
    }

    public static String getServerName(JavaPlugin plugin) {
        String configured = plugin == null ? null : plugin.getConfig().getString("server-name", "lobby");
        return ServerNameResolver.resolve(plugin == null ? null : plugin.getServer(), configured);
    }

    private static Method findOnlinePlayersMethod() {
        try {
            return Bukkit.class.getMethod("getOnlinePlayers");
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
