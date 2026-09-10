package cn.mx.lobbyaddon.chat;

import cn.mx.lobbyaddon.LobbyAddon;
import cn.mx.lobbyaddon.util.AtomicYamlFiles;
import cn.mx.lobbyaddon.util.BukkitVersion;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ChatColorService {

    private final LobbyAddon plugin;
    private final File dataFile;
    private final Map<UUID, ChatStyle> styles = new ConcurrentHashMap<>();
    private final Map<String, ChatStyle> presets = new LinkedHashMap<>();
    private final Map<String, String> presetPermissions = new LinkedHashMap<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "LobbyAddon-ChatColorStore");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean writeScheduled = new AtomicBoolean();
    private volatile Map<UUID, ChatStyle> pendingSnapshot;
    private volatile boolean closed;
    private volatile boolean healthy = true;

    public ChatColorService(LobbyAddon plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "chat-colors.yml");
    }

    public void start() {
        loadPresets();
        loadStyles();
    }

    public void close() {
        closed = true;
        ioExecutor.shutdown();
        try {
            ioExecutor.awaitTermination(3L, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        save(new LinkedHashMap<>(styles));
    }

    public ChatStyle getStyle(UUID uuid) {
        ChatStyle style = styles.get(uuid);
        return style == null ? ChatStyle.none() : style;
    }

    public void setStyle(UUID uuid, ChatStyle style) {
        if (style == null || style.isEmpty()) {
            styles.remove(uuid);
        } else {
            styles.put(uuid, style);
        }
        saveAsync();
    }

    public void reset(UUID uuid) {
        styles.remove(uuid);
        saveAsync();
    }

    public String render(Player player, String message) {
        boolean supportsHex = BukkitVersion.supportsHexColors(plugin.getServer().getBukkitVersion());
        ChatStyle effective = effectiveStyle(player, getStyle(player.getUniqueId()));
        return ChatMessageRenderer.render(message, effective, supportsHex,
                plugin.getConfig().getBoolean("chat-color.modern.enabled", true),
                plugin.getConfig().getBoolean("chat-color.modern.legacy-fallback", true));
    }

    public ChatStyle effectiveStyle(Player player, ChatStyle style) {
        boolean legacyAllowed = plugin.getConfig().getBoolean("chat-color.legacy.enabled", true)
                && hasConfiguredPermission(player, "legacy", "lobbyaddon.chatcolor.legacy")
                && isLegacyColorAllowed(style);
        return ChatStylePolicy.filter(style,
                hasConfiguredPermission(player, "use", "lobbyaddon.chatcolor.use"),
                legacyAllowed,
                modernFeatureEnabled("hex-enabled")
                        && hasConfiguredPermission(player, "hex", "lobbyaddon.chatcolor.hex"),
                modernFeatureEnabled("gradient-enabled")
                        && hasConfiguredPermission(player, "gradient", "lobbyaddon.chatcolor.gradient"),
                plugin.getConfig().getBoolean("chat-color.formats.enabled", true)
                        && hasConfiguredPermission(player, "format", "lobbyaddon.chatcolor.format"),
                allowedFormats(),
                hasConfiguredPermission(player, "obfuscated", "lobbyaddon.chatcolor.obfuscated"));
    }

    public ChatStyle getPreset(String name) {
        if (name == null) {
            return null;
        }
        return presets.get(name.toLowerCase(Locale.ROOT));
    }

    public List<String> getPresetNames() {
        return new ArrayList<>(presets.keySet());
    }

    public boolean isPresetAllowed(Player player, String name) {
        if (player == null || name == null || !presets.containsKey(name.toLowerCase(Locale.ROOT))) {
            return false;
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        ChatStyle style = presets.get(normalized);
        if (!sameStyle(style, effectiveStyle(player, style))) {
            return false;
        }
        String permission = presetPermissions.get(normalized);
        return permission == null || permission.trim().isEmpty() || player.hasPermission(permission);
    }

    private static boolean sameStyle(ChatStyle left, ChatStyle right) {
        return left.getMode() == right.getMode()
                && left.getLegacyColor() == right.getLegacyColor()
                && left.getPrimaryRgb() == right.getPrimaryRgb()
                && left.getSecondaryRgb() == right.getSecondaryRgb()
                && left.getFormats().equals(right.getFormats());
    }

    public boolean isHealthy() {
        return healthy;
    }

    public int size() {
        return styles.size();
    }

    private boolean modernFeatureEnabled(String key) {
        return plugin.getConfig().getBoolean("chat-color.modern.enabled", true)
                && plugin.getConfig().getBoolean("chat-color.modern." + key, true);
    }

    private boolean hasConfiguredPermission(Player player, String key, String defaultPermission) {
        String permission = plugin.getConfig().getString("chat-color.permissions." + key, defaultPermission);
        return permission == null || permission.trim().isEmpty() || player.hasPermission(permission);
    }

    private boolean isLegacyColorAllowed(ChatStyle style) {
        if (style == null || style.getMode() != ChatStyle.Mode.LEGACY) {
            return true;
        }
        List<String> configured = plugin.getConfig().getStringList("chat-color.legacy.allowed-colors");
        if (configured.isEmpty()) {
            return true;
        }
        for (String entry : configured) {
            Character color = ChatColorParser.parseLegacy(entry);
            if (color != null && color == style.getLegacyColor()) {
                return true;
            }
        }
        return false;
    }

    private Set<ChatFormat> allowedFormats() {
        List<String> configured = plugin.getConfig().getStringList("chat-color.formats.allowed");
        if (configured.isEmpty()) {
            return EnumSet.allOf(ChatFormat.class);
        }
        EnumSet<ChatFormat> formats = EnumSet.noneOf(ChatFormat.class);
        for (String entry : configured) {
            ChatFormat format = ChatFormat.fromInput(entry);
            if (format != null) {
                formats.add(format);
            }
        }
        return formats;
    }

    private void loadPresets() {
        presets.clear();
        presetPermissions.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("chat-color.presets");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection preset = section.getConfigurationSection(key);
            if (preset == null) {
                continue;
            }
            ChatStyle style = ChatStyleCodec.decode(preset.getString("mode"), preset.getString("primary"),
                    preset.getString("secondary"), preset.getStringList("formats"));
            if (!style.isEmpty()) {
                String normalized = key.toLowerCase(Locale.ROOT);
                presets.put(normalized, style);
                presetPermissions.put(normalized, preset.getString("permission", ""));
            }
        }
    }

    private void loadStyles() {
        styles.clear();
        if (!dataFile.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String base = "players." + key + ".";
                ChatStyle style = ChatStyleCodec.decode(yaml.getString(base + "mode"),
                        yaml.getString(base + "primary"), yaml.getString(base + "secondary"),
                        yaml.getStringList(base + "formats"));
                if (!style.isEmpty()) {
                    styles.put(uuid, style);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void saveAsync() {
        if (closed) {
            return;
        }
        pendingSnapshot = new LinkedHashMap<>(styles);
        if (!writeScheduled.compareAndSet(false, true)) {
            return;
        }
        ioExecutor.execute(() -> {
            try {
                while (!closed) {
                    Map<UUID, ChatStyle> snapshot = pendingSnapshot;
                    pendingSnapshot = null;
                    if (snapshot != null) {
                        save(snapshot);
                    }
                    if (pendingSnapshot == null) {
                        break;
                    }
                }
            } finally {
                writeScheduled.set(false);
                if (!closed && pendingSnapshot != null) {
                    saveAsync();
                }
            }
        });
    }

    private void save(Map<UUID, ChatStyle> values) {
        YamlConfiguration yaml = new YamlConfiguration();
        List<UUID> sorted = new ArrayList<>(values.keySet());
        Collections.sort(sorted, (left, right) -> left.toString().compareTo(right.toString()));
        for (UUID uuid : sorted) {
            ChatStyle style = values.get(uuid);
            String base = "players." + uuid + ".";
            yaml.set(base + "mode", ChatStyleCodec.mode(style));
            yaml.set(base + "primary", ChatStyleCodec.primary(style));
            yaml.set(base + "secondary", ChatStyleCodec.secondary(style));
            yaml.set(base + "formats", ChatStyleCodec.formats(style));
        }
        try {
            AtomicYamlFiles.save(yaml, dataFile);
            healthy = true;
        } catch (IOException exception) {
            healthy = false;
            plugin.getLogger().warning("Could not save chat-colors.yml: " + exception.getMessage());
        }
    }
}
