package cn.mx.lobbyaddon.profile;

import java.util.UUID;

public final class AuditEntry {

    private final long timestamp;
    private final UUID actorId;
    private final String actorName;
    private final UUID targetId;
    private final String targetName;
    private final ProfileType type;
    private final String action;
    private final String value;

    public AuditEntry(long timestamp, UUID actorId, String actorName, UUID targetId, String targetName,
                      ProfileType type, String action, String value) {
        this.timestamp = timestamp;
        this.actorId = actorId;
        this.actorName = actorName;
        this.targetId = targetId;
        this.targetName = targetName;
        this.type = type;
        this.action = action;
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public ProfileType getType() {
        return type;
    }

    public String getAction() {
        return action;
    }

    public String getValue() {
        return value;
    }
}
