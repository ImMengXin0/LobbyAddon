package cn.mx.lobbyaddon.profile;

public final class ProfileOperationResult {

    public enum Status {
        APPLIED,
        PENDING_APPROVAL,
        COOLDOWN,
        CONTEXT_BLOCKED,
        DEPENDENCY_UNAVAILABLE,
        TEMPORARY_DISABLED,
        STORAGE_UNAVAILABLE,
        DURATION_TOO_LONG,
        FEATURE_DISABLED,
        INVALID_VALUE,
        REQUEST_UNAVAILABLE
    }

    private final Status status;
    private final long remainingMillis;
    private final String requestId;
    private final String message;

    private ProfileOperationResult(Status status, long remainingMillis, String requestId, String message) {
        this.status = status;
        this.remainingMillis = remainingMillis;
        this.requestId = requestId;
        this.message = message;
    }

    public static ProfileOperationResult of(Status status) {
        return new ProfileOperationResult(status, 0L, null, null);
    }

    public static ProfileOperationResult cooldown(long remainingMillis) {
        return new ProfileOperationResult(Status.COOLDOWN, remainingMillis, null, null);
    }

    public static ProfileOperationResult pending(String requestId) {
        return new ProfileOperationResult(Status.PENDING_APPROVAL, 0L, requestId, null);
    }

    public static ProfileOperationResult invalid(String message) {
        return new ProfileOperationResult(Status.INVALID_VALUE, 0L, null, message);
    }

    public Status getStatus() {
        return status;
    }

    public long getRemainingMillis() {
        return remainingMillis;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getMessage() {
        return message;
    }
}
