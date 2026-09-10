package cn.mx.lobbyaddon.profile;

import java.util.UUID;

public final class ProfileRequest {

    private final String id;
    private final UUID requesterId;
    private final String requesterName;
    private final ProfileType type;
    private final String value;
    private final long durationMillis;
    private final long createdAt;
    private final long expiresAt;
    private final boolean colorAllowed;
    private final boolean lengthBypassAllowed;

    public ProfileRequest(String id, UUID requesterId, String requesterName, ProfileType type,
                          String value, long durationMillis, long createdAt, long expiresAt) {
        this(id, requesterId, requesterName, type, value, durationMillis, createdAt, expiresAt, false, false);
    }

    public ProfileRequest(String id, UUID requesterId, String requesterName, ProfileType type,
                          String value, long durationMillis, long createdAt, long expiresAt,
                          boolean colorAllowed, boolean lengthBypassAllowed) {
        this.id = id;
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.type = type;
        this.value = value;
        this.durationMillis = durationMillis;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.colorAllowed = colorAllowed;
        this.lengthBypassAllowed = lengthBypassAllowed;
    }

    public String getId() {
        return id;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public ProfileType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isColorAllowed() {
        return colorAllowed;
    }

    public boolean isLengthBypassAllowed() {
        return lengthBypassAllowed;
    }

    public boolean isExpired(long now) {
        return expiresAt > 0L && expiresAt <= now;
    }
}
