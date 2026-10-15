package cn.mx.lobbyaddon.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import cn.mx.lobbyaddon.LobbyAddon;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cache-first MySQL storage. Bukkit-facing reads never query the database;
 * database setup, hydration and writes are serialized on a dedicated I/O thread.
 */
public final class MysqlStorage implements DataStorage {

    private final LobbyAddon plugin;
    private final Map<UUID, String> nicks = new ConcurrentHashMap<>();
    private final Set<UUID> pendingWrites = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> dirtyUpserts = new ConcurrentHashMap<>();
    private final Set<UUID> dirtyDeletes = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean maintenanceQueued = new AtomicBoolean();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "LobbyAddon-MySQL");
        thread.setDaemon(true);
        return thread;
    });
    private final String host;
    private final String database;
    private final String username;
    private final String password;
    private final String table;
    private final int port;
    private final boolean useSSL;
    private final int poolSize;
    private final boolean migrateYamlOnFirstLoad;

    private volatile HikariDataSource dataSource;
    private volatile boolean closed;
    private volatile BukkitTask maintenanceTask;
    private volatile boolean ready;
    private volatile boolean healthy = true;
    private volatile String lastError;
    private volatile long lastRefreshAt;

    public MysqlStorage(LobbyAddon plugin) {
        this.plugin = plugin;
        this.host = plugin.getConfig().getString("mysql.host", "localhost");
        this.port = plugin.getConfig().getInt("mysql.port", 3306);
        this.database = plugin.getConfig().getString("mysql.database", "lobbyaddon");
        this.username = plugin.getConfig().getString("mysql.username", "root");
        this.password = plugin.getConfig().getString("mysql.password", "");
        String configuredTable = plugin.getConfig().getString("mysql.table", "nick_data");
        this.table = configuredTable != null && configuredTable.matches("[A-Za-z0-9_]+") ? configuredTable : "nick_data";
        this.useSSL = plugin.getConfig().getBoolean("mysql.useSSL", false);
        this.poolSize = Math.max(1, plugin.getConfig().getInt("mysql.pool-size", 8));
        this.migrateYamlOnFirstLoad = plugin.getConfig().getBoolean("mysql.migrate-yaml-on-first-load", false);
    }

    @Override
    public void init() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=" + useSSL + "&useUnicode=true&characterEncoding=UTF-8&socketTimeout=30000");
        config.setUsername(username);
        long interval = Math.max(5L, plugin.getConfig().getLong("mysql.cache-refresh-seconds", 15L)) * 20L;
        maintenanceTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                this::queueMaintenance, interval, interval);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(5000L);
        config.setValidationTimeout(3000L);
        config.setInitializationFailTimeout(-1L);
        config.setPoolName("LobbyAddon-MySQL");
        dataSource = new HikariDataSource(config);
        ioExecutor.execute(this::initializeAndLoad);
    }

    @Override
    public String getNick(UUID uuid) {
        return nicks.get(uuid);
    }

    @Override
    public void setNick(UUID uuid, String rawNick) {
        if (rawNick == null) {
            removeNick(uuid);
            return;
        }
        nicks.put(uuid, rawNick);
        pendingWrites.add(uuid);
        dirtyDeletes.remove(uuid);
        dirtyUpserts.put(uuid, rawNick);
        queueMaintenance();
    }

    @Override
    public void removeNick(UUID uuid) {
        nicks.remove(uuid);
        pendingWrites.add(uuid);
        dirtyUpserts.remove(uuid);
        dirtyDeletes.add(uuid);
        queueMaintenance();
    }

    @Override
    public Map<UUID, String> getAllNicks() {
        return new HashMap<>(nicks);
    }

    @Override
    public UUID getUuidByNick(String nick) {
        String search = plugin.getNickManager().comparisonKey(nick);
        for (Map.Entry<UUID, String> entry : nicks.entrySet()) {
            if (plugin.getNickManager().comparisonKey(entry.getValue()).equals(search)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public void close() {
        if (maintenanceTask != null) {
            maintenanceTask.cancel();
            maintenanceTask = null;
        }
        closed = true;
        ioExecutor.shutdown();
        try {
            ioExecutor.awaitTermination(5L, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        HikariDataSource source = dataSource;
        if (source != null) {
            source.close();
            dataSource = null;
        }
    }

    private void initializeAndLoad() {
        try (Connection connection = connection()) {
            connection.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS `" + table + "` ("
                    + "`uuid` VARCHAR(36) PRIMARY KEY, `nick` VARCHAR(128) NOT NULL)");
            loadCache(connection);
            if (nicks.isEmpty() && migrateYamlOnFirstLoad) {
                migrateYamlNicknames(connection);
            }
            plugin.getLogger().info("MySQL cache loaded: " + nicks.size() + " nickname(s)");
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!closed && plugin.getStorage() == this && plugin.getDisplayService() != null) {
                    plugin.getDisplayService().refreshAll();
                }
            });
            ready = true;
            healthy = true;
            lastError = null;
        } catch (SQLException | IllegalArgumentException exception) {
            markFailure("MySQL initialization failed", exception);
        }
    }
    private void queueMaintenance() {
        if (closed || !maintenanceQueued.compareAndSet(false, true)) {
            return;
        }
        try {
            ioExecutor.execute(() -> {
                try {
                    runMaintenance();
                } finally {
                    maintenanceQueued.set(false);
                }
            });
        } catch (RejectedExecutionException ignored) {
            maintenanceQueued.set(false);
        }
    }

    private void runMaintenance() {
        try (Connection connection = connection()) {
            connection.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS `" + table + "` ("
                    + "`uuid` VARCHAR(36) PRIMARY KEY, `nick` VARCHAR(128) NOT NULL)");
            flushDirty(connection);
            loadCache(connection);
            healthy = true;
            lastError = null;
            ready = true;
        } catch (SQLException | IllegalArgumentException exception) {
            markFailure("MySQL maintenance failed", exception);
        }
    }

    private void flushDirty(Connection connection) throws SQLException {
        for (Map.Entry<UUID, String> entry : new HashMap<>(dirtyUpserts).entrySet()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO `" + table + "` (uuid,nick) VALUES (?,?) "
                            + "ON DUPLICATE KEY UPDATE nick=VALUES(nick)")) {
                statement.setString(1, entry.getKey().toString());
                statement.setString(2, entry.getValue());
                statement.executeUpdate();
                if (entry.getValue().equals(dirtyUpserts.get(entry.getKey()))) {
                    dirtyUpserts.remove(entry.getKey(), entry.getValue());
                    pendingWrites.remove(entry.getKey());
                }
            }
        }
        for (UUID uuid : new java.util.HashSet<>(dirtyDeletes)) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM `" + table + "` WHERE uuid = ?")) {
                statement.setString(1, uuid.toString());
                statement.executeUpdate();
                if (dirtyDeletes.remove(uuid)) {
                    pendingWrites.remove(uuid);
                }
            }
        }
    }

    private void loadCache(Connection connection) throws SQLException {
        Map<UUID, String> loaded = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT uuid,nick FROM `" + table + "`");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                loaded.put(uuid, resultSet.getString("nick"));
            }
        }
        for (Map.Entry<UUID, String> entry : loaded.entrySet()) {
            if (!pendingWrites.contains(entry.getKey())) {
                nicks.put(entry.getKey(), entry.getValue());
            }
        }
        for (UUID uuid : new java.util.HashSet<>(nicks.keySet())) {
            if (!loaded.containsKey(uuid) && !pendingWrites.contains(uuid)) {
                nicks.remove(uuid);
            }
        }
        lastRefreshAt = System.currentTimeMillis();
    }

    private void markFailure(String context, Exception exception) {
        healthy = false;
        lastError = exception.getMessage();
        plugin.getLogger().warning(context + ": " + exception.getMessage());
    }

    @Override public boolean isReady() { return ready; }
    @Override public boolean isHealthy() { return healthy; }

    @Override
    public String getStatus() {
        if (!ready) return "loading" + (lastError == null ? "" : " (" + lastError + ")");
        return healthy ? "ready, refreshed=" + lastRefreshAt
                : "degraded" + (lastError == null ? "" : " (" + lastError + ")");
    }

    private Connection connection() throws SQLException {
        HikariDataSource source = dataSource;
        if (source == null) {
            throw new SQLException("MySQL datasource is closed");
        }
        return source.getConnection();
    }

    private void migrateYamlNicknames(Connection connection) throws SQLException {
        File legacyFile = new File(plugin.getDataFolder(), "nicks.yml");
        if (!legacyFile.exists()) {
            return;
        }
        YamlConfiguration legacy = YamlConfiguration.loadConfiguration(legacyFile);
        try (PreparedStatement statement = connection.prepareStatement(
                "REPLACE INTO `" + table + "` (uuid,nick) VALUES (?,?)")) {
            int migrated = 0;
            for (String key : legacy.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String nickname = legacy.getString(key);
                    if (nickname == null) {
                        continue;
                    }
                    statement.setString(1, uuid.toString());
                    statement.setString(2, nickname);
                    statement.addBatch();
                    nicks.put(uuid, nickname);
                    migrated++;
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (migrated > 0) {
                statement.executeBatch();
                plugin.getLogger().info("Migrated " + migrated + " nickname(s) from nicks.yml to MySQL");
            }
        }
    }
}
