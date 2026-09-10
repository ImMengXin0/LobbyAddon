package cn.mx.lobbyaddon.config;

import cn.mx.lobbyaddon.LobbyAddon;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class LobbyConfig {

    // nick
    public int nickMinLength, nickMaxLength;
    public boolean colorEnabled, hexEnabled;
    public char colorCode;

    // custom
    public boolean customEnabled;
    public int customPriority, customMinLength, customMaxLength;
    public List<String> bannedWords;

    // suffix
    public boolean suffixEnabled;
    public int suffixPriority, suffixMinLength, suffixMaxLength;
    public List<String> suffixBannedWords;

    // perms
    public String nickUse, nickColor, nickOthers, nickBypass;
    public String customUse, customColor, customBypass;
    public String suffixUse, suffixColor, suffixBypass;

    // nochat
    public boolean nochatEnabled;
    public String nochatMessage;
    public String nochatBypassPerm;

    public LobbyConfig(LobbyAddon plugin) { load(plugin.getConfig()); }

    public void load(FileConfiguration c) {
        nickMinLength = c.getInt("nickname.min-length", 3);
        nickMaxLength = c.getInt("nickname.max-length", 16);
        colorEnabled  = c.getBoolean("color.enabled", true);
        hexEnabled    = c.getBoolean("color.hex-enabled", true);
        String code   = c.getString("color.code-char", "&");
        colorCode     = code.isEmpty() ? '&' : code.charAt(0);

        customEnabled  = c.getBoolean("custom.enabled", true);
        customPriority = c.getInt("custom.prefix-priority", 100);
        customMinLength = c.getInt("custom.min-length", 1);
        customMaxLength = c.getInt("custom.max-length", 16);
        bannedWords     = c.getStringList("custom.banned-words");

        suffixEnabled = c.getBoolean("suffix.enabled", true);
        suffixPriority = c.getInt("suffix.priority", 101);
        suffixMinLength = c.getInt("suffix.min-length", 1);
        suffixMaxLength = c.getInt("suffix.max-length", 16);
        suffixBannedWords = c.getStringList("suffix.banned-words");

        nickUse     = c.getString("permissions.nick-use",      "lobbyaddon.nick.use");
        nickColor   = c.getString("permissions.nick-color",    "lobbyaddon.nick.color");
        nickOthers  = c.getString("permissions.nick-others",   "lobbyaddon.nick.others");
        nickBypass  = c.getString("permissions.nick-bypass",   "lobbyaddon.nick.bypass");
        customUse   = c.getString("permissions.custom-use",    "lobbyaddon.custom.use");
        customColor = c.getString("permissions.custom-color",  "lobbyaddon.custom.color");
        customBypass = c.getString("permissions.custom-bypass","lobbyaddon.custom.bypass");
        suffixUse = c.getString("permissions.suffix-use", "lobbyaddon.suffix.use");
        suffixColor = c.getString("permissions.suffix-color", "lobbyaddon.suffix.color");
        suffixBypass = c.getString("permissions.suffix-bypass", "lobbyaddon.suffix.bypass");

        nochatEnabled   = c.getBoolean("nochat.enabled", true);
        nochatMessage   = c.getString("nochat.message", "&c登录服内禁止聊天。");
        nochatBypassPerm = c.getString("nochat.bypass-permission", "lobbyaddon.nochat.bypass");
    }
}
