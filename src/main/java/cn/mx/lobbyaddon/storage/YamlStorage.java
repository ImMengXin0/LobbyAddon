package cn.mx.lobbyaddon.storage;

import cn.mx.lobbyaddon.LobbyAddon;
import cn.mx.lobbyaddon.util.AtomicYamlFiles;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class YamlStorage implements DataStorage {

    private final LobbyAddon plugin;
    private final Map<UUID, String> nicks = new ConcurrentHashMap<>();
    private final File nickFile;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "LobbyAddon-YamlStore");
        thread.setDaemon(true);
        return thread;
    });
    private volatile boolean closed;
    private final AtomicBoolean writeScheduled = new AtomicBoolean();
    private volatile Map<UUID, String> pendingSnapshot;
    private volatile boolean healthy = true;

    public YamlStorage(LobbyAddon plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        this.nickFile = new File(plugin.getDataFolder(), "nicks.yml");
    }

    @Override
    public void init() {
        if (!nickFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(nickFile);
        for (String key : cfg.getKeys(false)) {
            try { nicks.put(UUID.fromString(key), cfg.getString(key)); }
            catch (IllegalArgumentException ignored) {}
        }
    }

    @Override public String getNick(UUID u) { return nicks.get(u); }
    @Override public void setNick(UUID u, String n) { nicks.put(u, n); saveAsync(); }
    @Override public void removeNick(UUID u) { nicks.remove(u); saveAsync(); }
    @Override public Map<UUID, String> getAllNicks() { return new ConcurrentHashMap<>(nicks); }

    @Override
    public UUID getUuidByNick(String nick) {
        String test = plugin.getNickManager().comparisonKey(nick);
        for (Map.Entry<UUID, String> e : nicks.entrySet()) {
            if (plugin.getNickManager().comparisonKey(e.getValue()).equals(test)) return e.getKey();
        }
        return null;
    }

    @Override
    public void close() {
        closed = true;
        ioExecutor.shutdown();
        try {
            ioExecutor.awaitTermination(3L, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        save();
    }

    private void saveAsync() {
        if (closed) return;
        pendingSnapshot = new ConcurrentHashMap<>(nicks);
        if (!writeScheduled.compareAndSet(false, true)) return;
        ioExecutor.execute(() -> {
            try {
                while (!closed) {
                    Map<UUID, String> snapshot = pendingSnapshot;
                    pendingSnapshot = null;
                    if (snapshot != null) save(snapshot);
                    if (pendingSnapshot == null) break;
                }
            } finally {
                writeScheduled.set(false);
                if (!closed && pendingSnapshot != null) saveAsync();
            }
        });
    }

    private void save() {
        save(new ConcurrentHashMap<>(nicks));
    }

    private void save(Map<UUID, String> values) {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, String> e : values.entrySet()) cfg.set(e.getKey().toString(), e.getValue());
        try {
            AtomicYamlFiles.save(cfg, nickFile);
            healthy = true;
        } catch (IOException ex) {
            healthy = false;
            plugin.getLogger().warning("YAML save: " + ex.getMessage());
        }
    }

    @Override public boolean isHealthy() { return healthy; }
}
