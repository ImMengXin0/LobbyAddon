package cn.mx.lobbyaddon.storage;

import java.util.Map;
import java.util.UUID;

public interface DataStorage {
    void init();
    String getNick(UUID uuid);
    void setNick(UUID uuid, String rawNick);
    void removeNick(UUID uuid);
    Map<UUID, String> getAllNicks();
    UUID getUuidByNick(String nick);
    void close();

    default boolean isReady() {
        return true;
    }

    default boolean isHealthy() {
        return true;
    }

    default String getStatus() {
        return isReady() ? isHealthy() ? "ready" : "degraded" : "loading";
    }
}
