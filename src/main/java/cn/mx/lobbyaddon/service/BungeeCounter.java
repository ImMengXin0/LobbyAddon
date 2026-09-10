package cn.mx.lobbyaddon.service;

import cn.mx.lobbyaddon.LobbyAddon;
import cn.mx.lobbyaddon.util.BukkitCompat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BungeeCord 在线人数统计
 * 通过 BungeeCord 插件消息通道请求各子服/全部在线人数并缓存。
 */
public class BungeeCounter implements PluginMessageListener {

    private static final String CHANNEL = "BungeeCord";

    private final LobbyAddon plugin;
    private final Map<String, CountCache> serverCounts = new ConcurrentHashMap<>();
    private volatile int totalPlayers = -1;
    private volatile long totalUpdatedAt;
    private BukkitTask task;

    public BungeeCounter(LobbyAddon plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
        int interval = Math.max(1, plugin.getConfig().getInt("bungee.request-interval", 3)) * 20;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::requestCounts, 20L, interval);
    }

    public void stop() {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** 向代理请求 ALL 与各子服人数 */
    private void requestCounts() {
        Player any = firstOnline();
        if (any == null) return;
        send(any, "PlayerCount", "ALL");
        for (String server : plugin.getConfig().getStringList("bungee.servers")) {
            if (server != null && !server.trim().isEmpty()) {
                send(any, "PlayerCount", server.trim());
            }
        }
    }

    private void send(Player player, String... parts) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            for (String part : parts) out.writeUTF(part);
            out.flush();
            player.sendPluginMessage(plugin, CHANNEL, bytes.toByteArray());
        } catch (IOException ignored) {
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel)) return;
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            String sub = in.readUTF();
            if (!sub.equals("PlayerCount")) return;
            String server = in.readUTF();
            int count = in.readInt();
            if (server.equals("ALL")) {
                totalPlayers = count;
                totalUpdatedAt = System.currentTimeMillis();
            } else {
                serverCounts.put(server, new CountCache(count, System.currentTimeMillis()));
            }
        } catch (IOException ignored) {
        }
    }

    /** 全部子服总在线（缓存失败时回退本地人数） */
    public int getTotal() {
        if (totalPlayers >= 0 && isFresh(totalUpdatedAt)) return totalPlayers;
        return BukkitCompat.getOnlinePlayerCount();
    }

    /** 指定子服在线（未获取到时回退本地人数或 -1） */
    public int getServerCount(String server) {
        if (server == null || server.trim().isEmpty()) {
            return -1;
        }
        String requested = server.trim();
        for (Map.Entry<String, CountCache> entry : serverCounts.entrySet()) {
            CountCache cached = entry.getValue();
            if (entry.getKey().equalsIgnoreCase(requested) && isFresh(cached.updatedAt)) {
                return cached.count;
            }
        }
        String localServer = BukkitCompat.getServerName(plugin);
        if (localServer != null && !localServer.isEmpty() && requested.equalsIgnoreCase(localServer)) {
            return BukkitCompat.getOnlinePlayerCount();
        }
        return -1;
    }

    public boolean isFresh() {
        return totalPlayers >= 0 && isFresh(totalUpdatedAt);
    }

    public long getLastUpdatedAt() {
        return totalUpdatedAt;
    }

    private boolean isFresh(long updatedAt) {
        long timeout = Math.max(5L, plugin.getConfig().getLong("bungee.cache-timeout-seconds", 15L)) * 1000L;
        return updatedAt > 0L && System.currentTimeMillis() - updatedAt <= timeout;
    }

    private Player firstOnline() {
        for (Player p : BukkitCompat.getOnlinePlayers()) return p;
        return null;
    }

    private static final class CountCache {
        private final int count;
        private final long updatedAt;

        private CountCache(int count, long updatedAt) {
            this.count = count;
            this.updatedAt = updatedAt;
        }
    }
}
