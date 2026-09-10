package cn.mx.lobbyaddon;

import cn.mx.lobbyaddon.command.CustomCommand;
import cn.mx.lobbyaddon.command.ChatColorCommand;
import cn.mx.lobbyaddon.chat.ChatColorService;
import cn.mx.lobbyaddon.command.CosmeticsCommand;
import cn.mx.lobbyaddon.command.LobbyCommand;
import cn.mx.lobbyaddon.command.NickCommand;
import cn.mx.lobbyaddon.command.RealnameCommand;
import cn.mx.lobbyaddon.command.SuffixCommand;
import cn.mx.lobbyaddon.config.ConfigDefaults;
import cn.mx.lobbyaddon.config.ConfigSanity;
import cn.mx.lobbyaddon.config.LobbyConfig;
import cn.mx.lobbyaddon.listener.NoChatListener;
import cn.mx.lobbyaddon.listener.PlayerListener;
import cn.mx.lobbyaddon.listener.PresetMenuListener;
import cn.mx.lobbyaddon.listener.PersonalMenuListener;
import cn.mx.lobbyaddon.menu.PersonalMenuService;
import cn.mx.lobbyaddon.menu.PresetMenuService;
import cn.mx.lobbyaddon.placeholder.LobbyPapiExpansion;
import cn.mx.lobbyaddon.profile.ProfileService;
import cn.mx.lobbyaddon.service.BungeeCounter;
import cn.mx.lobbyaddon.service.DisplayService;
import cn.mx.lobbyaddon.service.MenuUpdater;
import cn.mx.lobbyaddon.service.NickManager;
import cn.mx.lobbyaddon.util.Logo;

import cn.mx.lobbyaddon.storage.DataStorage;
import cn.mx.lobbyaddon.storage.MysqlStorage;
import cn.mx.lobbyaddon.storage.YamlStorage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class LobbyAddon extends JavaPlugin {

    private NickManager nickManager;
    private LobbyConfig config;
    private DataStorage storage;
    private MenuUpdater menuUpdater;
    private BungeeCounter bungeeCounter;
    private PlayerListener playerListener;
    private NoChatListener noChatListener;
    private ProfileService profileService;
    private DisplayService displayService;
    private boolean papiRegistered;
    private PresetMenuService presetMenuService;
    private PresetMenuListener presetMenuListener;
    private PersonalMenuService personalMenuService;
    private PersonalMenuListener personalMenuListener;
    private ChatColorService chatColorService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        load();
        Logo.print();

        if (!papiRegistered && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new LobbyPapiExpansion(this, "lobbyaddon").register();
            new LobbyPapiExpansion(this, "nick").register();
            papiRegistered = true;
            getLogger().info("PAPI expansion registered (lobbyaddon + nick)");
        }

        getLogger().info("LobbyAddon v" + getDescription().getVersion() + " enabled");
    }

    public void load() {
        reloadConfig();
        if (autoCompleteConfig()) {
            reloadConfig();
        }
        if (displayService != null) {
            displayService.clearAll();
        }
        stopRuntimeServices();
        config = new LobbyConfig(this);
        validateConfig();
        if (storage != null) storage.close();

        String storageType = getConfig().getString("storage.type", "yaml");
        storage = "mysql".equalsIgnoreCase(storageType) ? new MysqlStorage(this) : new YamlStorage(this);
        storage.init();
        nickManager = new NickManager(this, storage, config);
        profileService = new ProfileService(this, nickManager);
        profileService.start();
        displayService = new DisplayService(this, nickManager, profileService);
        presetMenuService = new PresetMenuService(this);
        chatColorService = new ChatColorService(this);
        chatColorService.start();
        personalMenuService = new PersonalMenuService(this);

        NickCommand nickCmd = new NickCommand(this, nickManager, config);
        getCommand("nick").setExecutor(nickCmd);
        getCommand("nick").setTabCompleter(nickCmd);

        CustomCommand customCmd = new CustomCommand(this, nickManager, config);
        getCommand("custom").setExecutor(customCmd);
        getCommand("custom").setTabCompleter(customCmd);

        SuffixCommand suffixCmd = new SuffixCommand(this, nickManager, config);
        getCommand("suffix").setExecutor(suffixCmd);
        getCommand("suffix").setTabCompleter(suffixCmd);

        getCommand("cosmetics").setExecutor(new CosmeticsCommand(this));

        ChatColorCommand chatColorCommand = new ChatColorCommand(this);
        getCommand("chatcolor").setExecutor(chatColorCommand);
        getCommand("chatcolor").setTabCompleter(chatColorCommand);

        getCommand("realname").setExecutor(new RealnameCommand(this, nickManager));
        LobbyCommand lobbyCmd = new LobbyCommand(this);
        getCommand("lobbyaddon").setExecutor(lobbyCmd);
        getCommand("lobbyaddon").setTabCompleter(lobbyCmd);

        // 先注销旧监听器，避免 reload 后重复注册
        if (playerListener != null) unregister(playerListener);
        if (noChatListener != null) unregister(noChatListener);

        playerListener = new PlayerListener(this);
        noChatListener = new NoChatListener(this, config);
        presetMenuListener = new PresetMenuListener(this);
        personalMenuListener = new PersonalMenuListener(this);
        getServer().getPluginManager().registerEvents(playerListener, this);
        getServer().getPluginManager().registerEvents(noChatListener, this);
        getServer().getPluginManager().registerEvents(presetMenuListener, this);
        getServer().getPluginManager().registerEvents(personalMenuListener, this);

        menuUpdater = new MenuUpdater(this);
        menuUpdater.start();

        if (getConfig().getBoolean("bungee.enabled", true)) {
            bungeeCounter = new BungeeCounter(this);
            bungeeCounter.start();
            getLogger().info("BungeeCord counter enabled");
        }
        displayService.refreshAll();
    }

    /** 从所有事件处理器列表注销指定监听器 */
    private void unregister(org.bukkit.event.Listener listener) {
        for (org.bukkit.event.HandlerList handlerList : org.bukkit.event.HandlerList.getHandlerLists()) {
            handlerList.unregister(listener);
        }
    }

    @Override
    public void onDisable() {
        if (displayService != null) {
            displayService.clearAll();
        }
        stopRuntimeServices();
        if (storage != null) storage.close();
    }

    private void stopRuntimeServices() {
        if (menuUpdater != null) {
            menuUpdater.stop();
            menuUpdater = null;
        }
        if (bungeeCounter != null) {
            bungeeCounter.stop();
            bungeeCounter = null;
        }
        if (profileService != null) {
            profileService.close();
            profileService = null;
        }
        if (chatColorService != null) {
            chatColorService.close();
            chatColorService = null;
        }
        if (personalMenuService != null) {
            personalMenuService.close();
            personalMenuService = null;
        }
        if (playerListener != null) {
            unregister(playerListener);
            playerListener = null;
        }
        if (noChatListener != null) {
            unregister(noChatListener);
            noChatListener = null;
        }
        if (presetMenuListener != null) {
            unregister(presetMenuListener);
            presetMenuListener = null;
        }
        if (personalMenuListener != null) {
            unregister(personalMenuListener);
            personalMenuListener = null;
        }
        displayService = null;
        presetMenuService = null;
    }

    /**
     * 配置自动补全：
     * 将 jar 内默认 config.yml 中缺失的配置项自动追加到现有配置文件。
     * 已有的配置项不会被覆盖（保留玩家自定义），仅在文件过旧时写入缺失项。
     */
    private boolean autoCompleteConfig() {
        try (java.io.InputStream defStream = getResource("config.yml")) {
            if (defStream == null) {
                return false;
            }
            org.bukkit.configuration.file.YamlConfiguration defaults =
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                            new java.io.InputStreamReader(defStream, java.nio.charset.StandardCharsets.UTF_8));

            org.bukkit.configuration.file.FileConfiguration current = getConfig();
            int added = copyMissingDefaults(defaults, current, "");

            if (added > 0) {
                saveConfig();
                getLogger().info("config.yml 自动补全了 " + added + " 个缺失配置项");
                getLogger().info("注意: 补全时会重写配置文件，原有注释已被移除，如需注释请参考默认配置");
            } else {
                getLogger().info("config.yml 检查完毕，无缺失配置项（无需更新）");
            }
            return added > 0;
        } catch (Exception e) {
            getLogger().warning("config.yml 自动补全失败: " + e.getMessage());
            return false;
        }
    }
    private void validateConfig() {
        String storageType = getConfig().getString("storage.type");
        if (!ConfigSanity.isStorageType(storageType)) {
            getLogger().warning("storage.type 仅支持 yaml 或 mysql，当前将使用 yaml");
        }
        String table = getConfig().getString("mysql.table");
        if (!ConfigSanity.isMysqlTable(table)) {
            getLogger().warning("mysql.table 只能包含字母、数字和下划线，当前将使用 nick_data");
        }
        validateLengthRange("nickname", config.nickMinLength, config.nickMaxLength);
        validateLengthRange("custom", config.customMinLength, config.customMaxLength);
        validateLengthRange("suffix", config.suffixMinLength, config.suffixMaxLength);

        int presetSize = getConfig().getInt("presets.size", 27);
        if (!ConfigSanity.isInventorySize(presetSize)) {
            getLogger().warning("presets.size 必须是 9 到 54 之间的 9 的倍数，菜单打开时将自动调整");
        }
        int maximumRawLength = getConfig().getInt("profile-rules.max-raw-length", 128);
        if (maximumRawLength < 16) {
            getLogger().warning("profile-rules.max-raw-length 不能小于 16，当前将按 16 处理");
        }

        String[] nonNegativePaths = {
                "cooldowns.nick", "cooldowns.prefix", "cooldowns.suffix",
                "menu-updater.interval", "bungee.request-interval", "bungee.cache-timeout-seconds",
                "temporary.max-duration-seconds", "temporary.check-interval-seconds",
                "approval.request-expiry-minutes", "history.limit", "mysql.cache-refresh-seconds",
                "personal-menu.input-timeout-seconds"
        };
        for (String path : nonNegativePaths) {
            if (!ConfigSanity.isNonNegative(getConfig().getLong(path))) {
                getLogger().warning(path + " 不能为负数，运行时将使用安全下限");
            }
        }
    }

    private void validateLengthRange(String section, int minimum, int maximum) {
        if (!ConfigSanity.isLengthRange(minimum, maximum)) {
            getLogger().warning(section + " 的 min-length/max-length 配置无效: "
                    + minimum + "/" + maximum);
        }
    }


    private boolean hasConfiguredValue(org.bukkit.configuration.ConfigurationSection section, String path) {
        return ConfigDefaults.hasExplicitValue(section, path);
    }

    private int copyMissingDefaults(org.bukkit.configuration.ConfigurationSection defaults,
                                    org.bukkit.configuration.ConfigurationSection current, String parentPath) {
        int added = 0;
        for (String key : defaults.getKeys(false)) {
            String path = parentPath.isEmpty() ? key : parentPath + "." + key;
            if (defaults.isConfigurationSection(key)) {
                org.bukkit.configuration.ConfigurationSection defaultSection = defaults.getConfigurationSection(key);
                if (hasConfiguredValue(current, path) && !current.isConfigurationSection(path)) {
                    getLogger().warning("Skipping incompatible config section: " + path);
                    continue;
                }
                if (defaultSection.getKeys(false).isEmpty()) {
                    if (!hasConfiguredValue(current, path)) {
                        current.createSection(path);
                        added++;
                    }
                } else {
                    added += copyMissingDefaults(defaultSection, current, path);
                }
            } else if (!hasConfiguredValue(current, path)) {
                current.set(path, defaults.get(key));
                added++;
            }
        }
        return added;
    }

    public NickManager getNickManager() { return nickManager; }
    public LobbyConfig getLobbyConfig() { return config; }
    public DataStorage getStorage() { return storage; }
    public BungeeCounter getBungeeCounter() { return bungeeCounter; }
    public ProfileService getProfileService() { return profileService; }
    public DisplayService getDisplayService() { return displayService; }
    public PresetMenuService getPresetMenuService() { return presetMenuService; }
    public PersonalMenuService getPersonalMenuService() { return personalMenuService; }
    public ChatColorService getChatColorService() { return chatColorService; }
    public void refreshDisplay(org.bukkit.entity.Player player) {
        if (displayService != null) displayService.refresh(player);
    }
}
