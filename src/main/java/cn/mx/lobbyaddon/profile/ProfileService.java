package cn.mx.lobbyaddon.profile;

import cn.mx.lobbyaddon.LobbyAddon;
import cn.mx.lobbyaddon.service.LuckPermsBridge;
import cn.mx.lobbyaddon.service.NickManager;
import cn.mx.lobbyaddon.util.AtomicYamlFiles;
import cn.mx.lobbyaddon.util.BukkitCompat;
import cn.mx.lobbyaddon.util.DurationParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public final class ProfileService {

    private final LobbyAddon plugin;
    private final NickManager nickManager;
    private final LuckPermsBridge luckPerms;
    private final ProfileValidator validator;
    private final File dataFile;
    private final Object lock = new Object();
    private final Map<UUID, AppearanceProfile> profiles = new HashMap<>();
    private final Map<String, ProfileRequest> requests = new LinkedHashMap<>();
    private final Deque<AuditEntry> history = new LinkedList<>();
    private final Map<UUID, EnumMap<ProfileType, Long>> cooldowns = new HashMap<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "LobbyAddon-ProfileStore");
        thread.setDaemon(true);
        return thread;
    });

    private BukkitTask expiryTask;
    private boolean closed;

    public ProfileService(LobbyAddon plugin, NickManager nickManager) {
        this.plugin = plugin;
        this.nickManager = nickManager;
        this.luckPerms = new LuckPermsBridge(plugin);
        this.validator = new ProfileValidator(plugin, nickManager);
        this.dataFile = new File(plugin.getDataFolder(), "profiles.yml");
    }

    public void start() {
        load();
        long interval = Math.max(15L, plugin.getConfig().getLong("temporary.check-interval-seconds", 30L)) * 20L;
        expiryTask = Bukkit.getScheduler().runTaskTimer(plugin, this::expireEntries, interval, interval);
    }

    public void close() {
        closed = true;
        if (expiryTask != null) {
            expiryTask.cancel();
            expiryTask = null;
        }
        ioExecutor.shutdown();
        try {
            ioExecutor.awaitTermination(3L, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        saveSync();
    }

    public ProfileOperationResult requestOrApply(Player player, ProfileType type, String value, long durationMillis) {
        if (type == ProfileType.NICK && !plugin.getStorage().isReady()) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.STORAGE_UNAVAILABLE);
        }
        if (!isTypeEnabled(type)) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.FEATURE_DISABLED);
        }
        if (!isAllowed(player, type)) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.CONTEXT_BLOCKED);
        }
        String validationError = validator.validate(player, type, value, player.getUniqueId());
        if (validationError != null) {
            return ProfileOperationResult.invalid(validationError);
        }
        ProfileOperationResult durationResult = validateTemporaryDuration(durationMillis);
        if (durationResult != null) {
            return durationResult;
        }
        if ((type == ProfileType.PREFIX || type == ProfileType.SUFFIX) && !luckPerms.isAvailable()) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.DEPENDENCY_UNAVAILABLE);
        }

        long remaining = getCooldownRemaining(player.getUniqueId(), type);
        if (remaining > 0L) {
            return ProfileOperationResult.cooldown(remaining);
        }

        if (requiresApproval(type)) {
            ProfileRequest request = createRequest(player, type, value, durationMillis);
            setCooldown(player.getUniqueId(), type);
            return ProfileOperationResult.pending(request.getId());
        }

        applyNow(player.getUniqueId(), player.getName(), player.getUniqueId(), player.getName(), type, value,
                durationMillis, "set");
        setCooldown(player.getUniqueId(), type);
        return ProfileOperationResult.of(ProfileOperationResult.Status.APPLIED);
    }

    public ProfileOperationResult reset(Player player, ProfileType type) {
        if (type == ProfileType.NICK && !plugin.getStorage().isReady()) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.STORAGE_UNAVAILABLE);
        }
        if (!isTypeEnabled(type)) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.FEATURE_DISABLED);
        }
        if (!isAllowed(player, type)) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.CONTEXT_BLOCKED);
        }
        if ((type == ProfileType.PREFIX || type == ProfileType.SUFFIX) && !luckPerms.isAvailable()) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.DEPENDENCY_UNAVAILABLE);
        }
        long remaining = getCooldownRemaining(player.getUniqueId(), type);
        if (remaining > 0L) {
            return ProfileOperationResult.cooldown(remaining);
        }
        clearNow(player.getUniqueId(), player.getName(), player.getUniqueId(), player.getName(), type, "reset");
        setCooldown(player.getUniqueId(), type);
        return ProfileOperationResult.of(ProfileOperationResult.Status.APPLIED);
    }

    public ProfileOperationResult applyAdministrative(Player actor, Player target, ProfileType type, String value,
                                                       long durationMillis) {
        if (type == ProfileType.NICK && !plugin.getStorage().isReady()) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.STORAGE_UNAVAILABLE);
        }
        if (!isTypeEnabled(type)) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.FEATURE_DISABLED);
        }
        if (!isAllowed(target, type)) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.CONTEXT_BLOCKED);
        }
        String validationError = validator.validate(actor, type, value, target.getUniqueId());
        if (validationError != null) {
            return ProfileOperationResult.invalid(validationError);
        }
        ProfileOperationResult durationResult = validateTemporaryDuration(durationMillis);
        if (durationResult != null) {
            return durationResult;
        }
        if ((type == ProfileType.PREFIX || type == ProfileType.SUFFIX) && !luckPerms.isAvailable()) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.DEPENDENCY_UNAVAILABLE);
        }
        applyNow(target.getUniqueId(), target.getName(), actor.getUniqueId(), actor.getName(), type, value,
                durationMillis, "admin-set");
        return ProfileOperationResult.of(ProfileOperationResult.Status.APPLIED);
    }

    public ProfileOperationResult resetAdministrative(Player actor, Player target, ProfileType type) {
        if (type == ProfileType.NICK && !plugin.getStorage().isReady()) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.STORAGE_UNAVAILABLE);
        }
        if (!isTypeEnabled(type)) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.FEATURE_DISABLED);
        }
        if (!isAllowed(target, type)) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.CONTEXT_BLOCKED);
        }
        if ((type == ProfileType.PREFIX || type == ProfileType.SUFFIX) && !luckPerms.isAvailable()) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.DEPENDENCY_UNAVAILABLE);
        }
        clearNow(target.getUniqueId(), target.getName(), actor.getUniqueId(), actor.getName(), type, "admin-reset");
        return ProfileOperationResult.of(ProfileOperationResult.Status.APPLIED);
    }

    public ProfileRequest getRequest(String requestId) {
        if (requestId == null) {
            return null;
        }
        synchronized (lock) {
            ProfileRequest exact = requests.get(requestId.toLowerCase(Locale.ROOT));
            if (exact != null) {
                return exact;
            }
            ProfileRequest match = null;
            for (ProfileRequest request : requests.values()) {
                if (request.getId().startsWith(requestId.toLowerCase(Locale.ROOT))) {
                    if (match != null) {
                        return null;
                    }
                    match = request;
                }
            }
            return match;
        }
    }

    public ProfileOperationResult approve(ProfileRequest request, UUID reviewerId, String reviewerName) {
        if (request == null || request.isExpired(System.currentTimeMillis())) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.REQUEST_UNAVAILABLE);
        }
        if (request.getType() == ProfileType.NICK && !plugin.getStorage().isReady()) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.STORAGE_UNAVAILABLE);
        }
        if (!isTypeEnabled(request.getType())) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.FEATURE_DISABLED);
        }
        if (!isServerAllowed(request.getType())) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.CONTEXT_BLOCKED);
        }
        ProfileOperationResult durationResult = validateTemporaryDuration(request.getDurationMillis());
        if (durationResult != null) {
            return durationResult;
        }
        if ((request.getType() == ProfileType.PREFIX || request.getType() == ProfileType.SUFFIX)
                && !luckPerms.isAvailable()) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.DEPENDENCY_UNAVAILABLE);
        }
        Player requester = Bukkit.getPlayer(request.getRequesterId());
        if (requester != null && !isAllowed(requester, request.getType())) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.CONTEXT_BLOCKED);
        }
        // Offline requests retain submission-time permission validation and are rechecked for shared rules.
        String validationError = validator.validateSubmitted(request);
        if (validationError != null) {
            return ProfileOperationResult.invalid(validationError);
        }
        synchronized (lock) {
            if (requests.remove(request.getId()) == null) {
                return ProfileOperationResult.of(ProfileOperationResult.Status.REQUEST_UNAVAILABLE);
            }
        }
        applyNow(request.getRequesterId(), request.getRequesterName(), reviewerId, reviewerName, request.getType(),
                request.getValue(), request.getDurationMillis(), "approved");
        persistAsync();
        return ProfileOperationResult.of(ProfileOperationResult.Status.APPLIED);
    }

    public boolean reject(ProfileRequest request, UUID reviewerId, String reviewerName) {
        if (request == null) {
            return false;
        }
        synchronized (lock) {
            if (requests.remove(request.getId()) == null) {
                return false;
            }
            recordLocked(reviewerId, reviewerName, request.getRequesterId(), request.getRequesterName(),
                    request.getType(), "rejected", request.getValue());
        }
        persistAsync();
        return true;
    }

    public List<ProfileRequest> getPendingRequests() {
        long now = System.currentTimeMillis();
        synchronized (lock) {
            List<ProfileRequest> result = new ArrayList<>();
            for (ProfileRequest request : requests.values()) {
                if (!request.isExpired(now)) {
                    result.add(request);
                }
            }
            result.sort(Comparator.comparingLong(ProfileRequest::getCreatedAt));
            return result;
        }
    }

    public List<AuditEntry> getHistory(UUID targetId, int limit) {
        synchronized (lock) {
            List<AuditEntry> result = new ArrayList<>();
            for (AuditEntry entry : history) {
                if (targetId == null || targetId.equals(entry.getTargetId())) {
                    result.add(entry);
                    if (result.size() >= limit) {
                        break;
                    }
                }
            }
            return result;
        }
    }

    public String getEffectiveNick(UUID uuid) {
        synchronized (lock) {
            AppearanceProfile profile = profiles.get(uuid);
            String temporary = profile == null ? null : profile.getEffectiveNick(System.currentTimeMillis());
            return temporary != null ? temporary : nickManager.getNick(uuid);
        }
    }

    public String getEffectivePrefix(UUID uuid) {
        synchronized (lock) {
            AppearanceProfile profile = profiles.get(uuid);
            return profile == null ? null : profile.getEffectivePrefix(System.currentTimeMillis());
        }
    }

    public String getEffectiveSuffix(UUID uuid) {
        synchronized (lock) {
            AppearanceProfile profile = profiles.get(uuid);
            return profile == null ? null : profile.getEffectiveSuffix(System.currentTimeMillis());
        }
    }

    public boolean hasTemporary(ProfileType type, UUID uuid) {
        synchronized (lock) {
            AppearanceProfile profile = profiles.get(uuid);
            if (profile == null) {
                return false;
            }
            long now = System.currentTimeMillis();
            if (type == ProfileType.NICK) {
                return profile.getTemporaryNick() != null && profile.getNickExpiresAt() > now;
            }
            if (type == ProfileType.PREFIX) {
                return profile.getTemporaryPrefix() != null && profile.getPrefixExpiresAt() > now;
            }
            return profile.getTemporarySuffix() != null && profile.getSuffixExpiresAt() > now;
        }
    }

    public long getExpiry(ProfileType type, UUID uuid) {
        synchronized (lock) {
            AppearanceProfile profile = profiles.get(uuid);
            if (profile == null) {
                return 0L;
            }
            long now = System.currentTimeMillis();
            if (type == ProfileType.NICK) {
                return profile.getTemporaryNick() != null && profile.getNickExpiresAt() > now
                        ? profile.getNickExpiresAt() : 0L;
            }
            if (type == ProfileType.PREFIX) {
                return profile.getTemporaryPrefix() != null && profile.getPrefixExpiresAt() > now
                        ? profile.getPrefixExpiresAt() : 0L;
            }
            return profile.getTemporarySuffix() != null && profile.getSuffixExpiresAt() > now
                    ? profile.getSuffixExpiresAt() : 0L;
        }
    }

    public UUID findUuidByNickname(String nickname) {
        if (nickname == null) {
            return null;
        }
        String search = nickManager.comparisonKey(nickname);
        synchronized (lock) {
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, AppearanceProfile> entry : profiles.entrySet()) {
                String temporary = entry.getValue().getEffectiveNick(now);
                if (temporary != null && nickManager.comparisonKey(temporary).equals(search)) {
                    return entry.getKey();
                }
            }
        }
        return nickManager.getUuidByNick(nickname);
    }

    public long getCooldownRemaining(UUID uuid, ProfileType type) {
        long seconds = Math.max(0L, plugin.getConfig().getLong("cooldowns." + type.getConfigKey(), 0L));
        if (seconds == 0L) {
            return 0L;
        }
        synchronized (lock) {
            EnumMap<ProfileType, Long> values = cooldowns.get(uuid);
            if (values == null || values.get(type) == null) {
                return 0L;
            }
            return Math.max(0L, values.get(type) + seconds * 1000L - System.currentTimeMillis());
        }
    }

    public boolean isLuckPermsAvailable() {
        return luckPerms.isAvailable();
    }

    public String validate(Player actor, ProfileType type, String value, UUID targetId) {
        return validator.validate(actor, type, value, targetId);
    }

    public String getDebugSummary() {
        synchronized (lock) {
            return "profiles=" + profiles.size() + ", pending=" + requests.size() + ", history=" + history.size()
                    + ", luckperms=" + luckPerms.isAvailable();
        }
    }

    public static long parseDurationMillis(String input) {
        return DurationParser.parseMillis(input);
    }

    public static String formatDuration(long millis) {
        long seconds = Math.max(1L, (millis + 999L) / 1000L);
        if (seconds >= 86_400L) {
            return (seconds / 86_400L) + "d";
        }
        if (seconds >= 3_600L) {
            return (seconds / 3_600L) + "h";
        }
        if (seconds >= 60L) {
            return (seconds / 60L) + "m";
        }
        return seconds + "s";
    }

    private ProfileRequest createRequest(Player player, ProfileType type, String value, long durationMillis) {
        long now = System.currentTimeMillis();
        long expiryMinutes = Math.max(1L, plugin.getConfig().getLong("approval.request-expiry-minutes", 1440L));
        ProfileRequest request = new ProfileRequest(UUID.randomUUID().toString().replace("-", "").toLowerCase(Locale.ROOT),
                player.getUniqueId(), player.getName(), type, value, durationMillis, now,
                now + expiryMinutes * 60_000L, hasColorPermission(player, type), hasBypassPermission(player, type));
        synchronized (lock) {
            List<String> oldIds = new ArrayList<>();
            for (ProfileRequest existing : requests.values()) {
                if (existing.getRequesterId().equals(player.getUniqueId()) && existing.getType() == type) {
                    oldIds.add(existing.getId());
                }
            }
            for (String oldId : oldIds) {
                requests.remove(oldId);
            }
            requests.put(request.getId(), request);
            recordLocked(player.getUniqueId(), player.getName(), player.getUniqueId(), player.getName(), type,
                    "requested", value);
        }
        for (Player reviewer : BukkitCompat.getOnlinePlayers()) {
            if (reviewer.hasPermission("lobbyaddon.approval.review")) {
                reviewer.sendMessage("§8[§bLobbyAddon§8] §e新的 " + type.getConfigKey() + " 审核申请: §f"
                        + player.getName() + " §7(#" + request.getId().substring(0, 8) + ")");
            }
        }
        persistAsync();
        return request;
    }

    private void applyNow(UUID targetId, String targetName, UUID actorId, String actorName, ProfileType type,
                          String value, long durationMillis, String action) {
        String previous;
        String next;
        synchronized (lock) {
            AppearanceProfile profile = getOrCreateProfileLocked(targetId);
            previous = getEffectiveValueLocked(profile, type, System.currentTimeMillis());
            long expiresAt = durationMillis > 0L ? System.currentTimeMillis() + durationMillis : 0L;
            if (type == ProfileType.NICK) {
                if (durationMillis > 0L) {
                    profile.setTemporaryNick(value, expiresAt);
                } else {
                    nickManager.setNick(targetId, value);
                    profile.setTemporaryNick(null, 0L);
                }
            } else if (type == ProfileType.PREFIX) {
                if (durationMillis > 0L) {
                    profile.setTemporaryPrefix(value, expiresAt);
                } else {
                    profile.setPrefix(value);
                    profile.setTemporaryPrefix(null, 0L);
                }
            } else {
                if (durationMillis > 0L) {
                    profile.setTemporarySuffix(value, expiresAt);
                } else {
                    profile.setSuffix(value);
                    profile.setTemporarySuffix(null, 0L);
                }
            }
            next = getEffectiveValueLocked(profile, type, System.currentTimeMillis());
            recordLocked(actorId, actorName, targetId, targetName, type, action, value);
        }
        syncLuckPerms(targetName, type, previous, next);
        refresh(targetId);
        persistAsync();
    }

    private void clearNow(UUID targetId, String targetName, UUID actorId, String actorName, ProfileType type,
                          String action) {
        String previous;
        synchronized (lock) {
            AppearanceProfile profile = getOrCreateProfileLocked(targetId);
            previous = getEffectiveValueLocked(profile, type, System.currentTimeMillis());
            if (type == ProfileType.NICK) {
                nickManager.removeNick(targetId);
                profile.setTemporaryNick(null, 0L);
            } else if (type == ProfileType.PREFIX) {
                profile.setPrefix(null);
                profile.setTemporaryPrefix(null, 0L);
            } else {
                profile.setSuffix(null);
                profile.setTemporarySuffix(null, 0L);
            }
            if (profile.isEmpty()) {
                profiles.remove(targetId);
            }
            recordLocked(actorId, actorName, targetId, targetName, type, action, null);
        }
        syncLuckPerms(targetName, type, previous, null);
        refresh(targetId);
        persistAsync();
    }

    private void expireEntries() {
        long now = System.currentTimeMillis();
        List<Expiration> expired = new ArrayList<>();
        boolean requestsExpired;
        synchronized (lock) {
            for (Map.Entry<UUID, AppearanceProfile> entry : profiles.entrySet()) {
                AppearanceProfile profile = entry.getValue();
                boolean nickExpired = profile.getTemporaryNick() != null && profile.getNickExpiresAt() <= now;
                boolean prefixExpired = profile.getTemporaryPrefix() != null && profile.getPrefixExpiresAt() <= now;
                boolean suffixExpired = profile.getTemporarySuffix() != null && profile.getSuffixExpiresAt() <= now;
                if (!nickExpired && !prefixExpired && !suffixExpired) {
                    continue;
                }
                String oldPrefix = prefixExpired ? profile.getTemporaryPrefix() : null;
                String oldSuffix = suffixExpired ? profile.getTemporarySuffix() : null;
                profile.clearExpired(now);
                expired.add(new Expiration(entry.getKey(), oldPrefix, oldSuffix, prefixExpired, suffixExpired, nickExpired));
                if (profile.isEmpty()) {
                    // Removal is deferred until the loop completes to keep iteration safe.
                }
            }
            for (Expiration expiration : expired) {
                AppearanceProfile profile = profiles.get(expiration.uuid);
                if (profile != null && profile.isEmpty()) {
                    profiles.remove(expiration.uuid);
                }
                String targetName = playerName(expiration.uuid);
                if (expiration.nickExpired) {
                    recordLocked(null, "system", expiration.uuid, targetName, ProfileType.NICK, "expired", null);
                }
                if (expiration.prefixExpired) {
                    recordLocked(null, "system", expiration.uuid, targetName, ProfileType.PREFIX, "expired", null);
                }
                if (expiration.suffixExpired) {
                    recordLocked(null, "system", expiration.uuid, targetName, ProfileType.SUFFIX, "expired", null);
                }
            }
            int requestCount = requests.size();
            requests.values().removeIf(request -> request.isExpired(now));
            requestsExpired = requests.size() != requestCount;
        }
        for (Expiration expiration : expired) {
            AppearanceProfile profile;
            synchronized (lock) {
                profile = profiles.get(expiration.uuid);
            }
            String name = playerName(expiration.uuid);
            if (expiration.prefixExpired) {
                syncLuckPerms(name, ProfileType.PREFIX, expiration.oldPrefix,
                        profile == null ? null : profile.getEffectivePrefix(now));
            }
            if (expiration.suffixExpired) {
                syncLuckPerms(name, ProfileType.SUFFIX, expiration.oldSuffix,
                        profile == null ? null : profile.getEffectiveSuffix(now));
            }
            refresh(expiration.uuid);
        }
        if (!expired.isEmpty() || requestsExpired) {
            persistAsync();
        }
    }

    private void syncLuckPerms(String playerName, ProfileType type, String previousRaw, String nextRaw) {
        if (playerName == null || type == ProfileType.NICK) {
            return;
        }
        if (type == ProfileType.PREFIX) {
            luckPerms.replacePrefix(playerName, formatPrefix(previousRaw), formatPrefix(nextRaw),
                    plugin.getLobbyConfig().customPriority);
        } else {
            luckPerms.replaceSuffix(playerName, formatSuffix(previousRaw), formatSuffix(nextRaw),
                    plugin.getLobbyConfig().suffixPriority);
        }
    }

    private String formatPrefix(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String value = nickManager.colorize(raw);
        return value.endsWith(" ") ? value : value + " ";
    }

    public String formatSuffix(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        return nickManager.colorize("&7[" + raw + "&7]");
    }

    private boolean requiresApproval(ProfileType type) {
        if (!plugin.getConfig().getBoolean("approval.enabled", false)) {
            return false;
        }
        for (String configured : plugin.getConfig().getStringList("approval.required-types")) {
            if (type.getConfigKey().equalsIgnoreCase(configured)) {
                return true;
            }
        }
        return false;
    }

    private ProfileOperationResult validateTemporaryDuration(long durationMillis) {
        if (durationMillis <= 0L) {
            return null;
        }
        if (!plugin.getConfig().getBoolean("temporary.enabled", true)) {
            return ProfileOperationResult.of(ProfileOperationResult.Status.TEMPORARY_DISABLED);
        }
        long maximum = Math.max(1L, plugin.getConfig().getLong("temporary.max-duration-seconds", 604800L)) * 1000L;
        return durationMillis > maximum
                ? ProfileOperationResult.of(ProfileOperationResult.Status.DURATION_TOO_LONG) : null;
    }

    public boolean isAllowed(Player player, ProfileType type) {
        if (player == null || !isTypeEnabled(type) || !isServerAllowed(type)) {
            return false;
        }
        if (containsIgnoreCase(plugin.getConfig().getStringList("profile-rules.disabled-worlds." + type.getConfigKey()),
                player.getWorld().getName())) {
            return false;
        }
        return true;
    }

    private boolean isServerAllowed(ProfileType type) {
        return !containsIgnoreCase(plugin.getConfig().getStringList("profile-rules.disabled-servers." + type.getConfigKey()),
                BukkitCompat.getServerName(plugin));
    }

    private boolean isTypeEnabled(ProfileType type) {
        if (type == ProfileType.PREFIX) {
            return plugin.getLobbyConfig().customEnabled;
        }
        return type != ProfileType.SUFFIX || plugin.getLobbyConfig().suffixEnabled;
    }

    private void setCooldown(UUID uuid, ProfileType type) {
        if (plugin.getConfig().getLong("cooldowns." + type.getConfigKey(), 0L) <= 0L) {
            return;
        }
        synchronized (lock) {
            cooldowns.computeIfAbsent(uuid, ignored -> new EnumMap<>(ProfileType.class))
                    .put(type, System.currentTimeMillis());
        }
    }

    private AppearanceProfile getOrCreateProfileLocked(UUID uuid) {
        AppearanceProfile profile = profiles.get(uuid);
        if (profile == null) {
            profile = new AppearanceProfile();
            profiles.put(uuid, profile);
        }
        return profile;
    }

    private String getEffectiveValueLocked(AppearanceProfile profile, ProfileType type, long now) {
        if (type == ProfileType.PREFIX) {

            return profile.getEffectivePrefix(now);
        }
        if (type == ProfileType.SUFFIX) {
            return profile.getEffectiveSuffix(now);
        }
        return profile.getEffectiveNick(now);
    }

    private boolean hasColorPermission(Player player, ProfileType type) {
        if (type == ProfileType.NICK) {
            return player.hasPermission(plugin.getLobbyConfig().nickColor);
        }
        if (type == ProfileType.PREFIX) {
            return player.hasPermission(plugin.getLobbyConfig().customColor);
        }
        return player.hasPermission(plugin.getLobbyConfig().suffixColor);
    }

    private boolean hasBypassPermission(Player player, ProfileType type) {
        if (type == ProfileType.NICK) {
            return player.hasPermission(plugin.getLobbyConfig().nickBypass);
        }
        if (type == ProfileType.PREFIX) {
            return player.hasPermission(plugin.getLobbyConfig().customBypass);
        }
        return player.hasPermission(plugin.getLobbyConfig().suffixBypass);
    }

    private void recordLocked(UUID actorId, String actorName, UUID targetId, String targetName,
                              ProfileType type, String action, String value) {
        history.addFirst(new AuditEntry(System.currentTimeMillis(), actorId, actorName, targetId, targetName,
                type, action, value));
        int limit = Math.max(1, plugin.getConfig().getInt("history.limit", 100));
        while (history.size() > limit) {
            history.removeLast();
        }
    }

    private void refresh(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            plugin.refreshDisplay(player);
        }
    }

    private String playerName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return offlinePlayer.getName() == null ? uuid.toString() : offlinePlayer.getName();
    }

    private void load() {
        if (!dataFile.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        synchronized (lock) {
            profiles.clear();
            requests.clear();
            history.clear();
            ConfigurationSection profilesSection = yaml.getConfigurationSection("profiles");
            if (profilesSection != null) {
                for (String key : profilesSection.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        ConfigurationSection section = profilesSection.getConfigurationSection(key);
                        if (section == null) {
                            continue;
                        }
                        AppearanceProfile profile = new AppearanceProfile();
                        profile.setPrefix(section.getString("prefix"));
                        profile.setSuffix(section.getString("suffix"));
                        profile.setTemporaryNick(section.getString("temporary-nick"), section.getLong("nick-expires-at"));
                        profile.setTemporaryPrefix(section.getString("temporary-prefix"), section.getLong("prefix-expires-at"));
                        profile.setTemporarySuffix(section.getString("temporary-suffix"), section.getLong("suffix-expires-at"));
                        if (!profile.isEmpty()) {
                            profiles.put(uuid, profile);
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            ConfigurationSection requestsSection = yaml.getConfigurationSection("requests");
            if (requestsSection != null) {
                for (String id : requestsSection.getKeys(false)) {
                    ConfigurationSection section = requestsSection.getConfigurationSection(id);
                    if (section == null) {
                        continue;
                    }
                    try {
                        UUID requester = UUID.fromString(section.getString("requester-id", ""));
                        ProfileType type = ProfileType.fromInput(section.getString("type"));
                        if (type == null) {
                            continue;
                        }
                        ProfileRequest request = new ProfileRequest(id, requester,
                                section.getString("requester-name", requester.toString()), type,
                                section.getString("value", ""), section.getLong("duration-millis"),
                                section.getLong("created-at"), section.getLong("expires-at"),
                                section.getBoolean("color-allowed", false),
                                section.getBoolean("length-bypass-allowed", false));
                        if (!request.isExpired(System.currentTimeMillis())) {
                            requests.put(id.toLowerCase(Locale.ROOT), request);
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            for (Map<?, ?> raw : yaml.getMapList("history")) {
                try {
                    ProfileType type = ProfileType.fromInput(String.valueOf(raw.get("type")));
                    if (type == null) {
                        continue;
                    }
                    history.addLast(new AuditEntry(asLong(raw.get("timestamp")), asUuid(raw.get("actor-id")),
                            asString(raw.get("actor-name")), asUuid(raw.get("target-id")),
                            asString(raw.get("target-name")), type, asString(raw.get("action")),
                            asString(raw.get("value"))));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    private void persistAsync() {
        if (closed) {
            return;
        }
        Snapshot snapshot = snapshot();
        try {
            ioExecutor.execute(() -> writeSnapshot(snapshot));
        } catch (RejectedExecutionException ignored) {
        }
    }

    private void saveSync() {
        writeSnapshot(snapshot());
    }

    private Snapshot snapshot() {
        synchronized (lock) {
            Map<UUID, AppearanceProfile> profileCopies = new LinkedHashMap<>();
            for (Map.Entry<UUID, AppearanceProfile> entry : profiles.entrySet()) {
                profileCopies.put(entry.getKey(), entry.getValue().copy());
            }
            return new Snapshot(profileCopies, new ArrayList<>(requests.values()), new ArrayList<>(history));
        }
    }

    private void writeSnapshot(Snapshot snapshot) {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, AppearanceProfile> entry : snapshot.profiles.entrySet()) {
            String base = "profiles." + entry.getKey() + ".";
            AppearanceProfile profile = entry.getValue();
            if (profile.getPrefix() != null) {
                yaml.set(base + "prefix", profile.getPrefix());
            }
            if (profile.getSuffix() != null) {
                yaml.set(base + "suffix", profile.getSuffix());
            }
            if (profile.getTemporaryNick() != null) {
                yaml.set(base + "temporary-nick", profile.getTemporaryNick());
                yaml.set(base + "nick-expires-at", profile.getNickExpiresAt());
            }
            if (profile.getTemporaryPrefix() != null) {
                yaml.set(base + "temporary-prefix", profile.getTemporaryPrefix());
                yaml.set(base + "prefix-expires-at", profile.getPrefixExpiresAt());
            }
            if (profile.getTemporarySuffix() != null) {
                yaml.set(base + "temporary-suffix", profile.getTemporarySuffix());
                yaml.set(base + "suffix-expires-at", profile.getSuffixExpiresAt());
            }
        }
        for (ProfileRequest request : snapshot.requests) {
            String base = "requests." + request.getId() + ".";
            yaml.set(base + "requester-id", request.getRequesterId().toString());
            yaml.set(base + "requester-name", request.getRequesterName());
            yaml.set(base + "type", request.getType().getConfigKey());
            yaml.set(base + "value", request.getValue());
            yaml.set(base + "duration-millis", request.getDurationMillis());
            yaml.set(base + "created-at", request.getCreatedAt());
            yaml.set(base + "expires-at", request.getExpiresAt());
            yaml.set(base + "color-allowed", request.isColorAllowed());
            yaml.set(base + "length-bypass-allowed", request.isLengthBypassAllowed());
        }
        List<Map<String, Object>> auditValues = new ArrayList<>();
        for (AuditEntry entry : snapshot.history) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("timestamp", entry.getTimestamp());
            value.put("actor-id", entry.getActorId() == null ? null : entry.getActorId().toString());
            value.put("actor-name", entry.getActorName());
            value.put("target-id", entry.getTargetId() == null ? null : entry.getTargetId().toString());
            value.put("target-name", entry.getTargetName());
            value.put("type", entry.getType().getConfigKey());
            value.put("action", entry.getAction());
            value.put("value", entry.getValue());
            auditValues.add(value);
        }
        yaml.set("history", auditValues);
        try {
            AtomicYamlFiles.save(yaml, dataFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save profiles.yml: " + exception.getMessage());
        }
    }

    private static boolean containsIgnoreCase(List<String> values, String target) {
        for (String value : values) {
            if (value.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    private static String asString(Object value) {
        return value == null || "null".equals(String.valueOf(value)) ? null : String.valueOf(value);
    }

    private static long asLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static UUID asUuid(Object value) {
        String text = asString(value);
        return text == null ? null : UUID.fromString(text);
    }

    private static final class Snapshot {
        private final Map<UUID, AppearanceProfile> profiles;
        private final List<ProfileRequest> requests;
        private final List<AuditEntry> history;

        private Snapshot(Map<UUID, AppearanceProfile> profiles, List<ProfileRequest> requests,
                         List<AuditEntry> history) {
            this.profiles = profiles;
            this.requests = requests;
            this.history = history;
        }
    }

    private static final class Expiration {
        private final UUID uuid;
        private final String oldPrefix;
        private final String oldSuffix;
        private final boolean prefixExpired;
        private final boolean suffixExpired;
        private final boolean nickExpired;

        private Expiration(UUID uuid, String oldPrefix, String oldSuffix, boolean prefixExpired,
                           boolean suffixExpired, boolean nickExpired) {
            this.uuid = uuid;
            this.oldPrefix = oldPrefix;
            this.oldSuffix = oldSuffix;
            this.prefixExpired = prefixExpired;
            this.suffixExpired = suffixExpired;
            this.nickExpired = nickExpired;
        }
    }
}
