package cn.mx.lobbyaddon.service;

import cn.mx.lobbyaddon.LobbyAddon;
import cn.mx.lobbyaddon.config.LobbyConfig;
import cn.mx.lobbyaddon.util.BukkitVersion;
import cn.mx.lobbyaddon.util.ProfileTextRules;

import cn.mx.lobbyaddon.storage.DataStorage;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NickManager {

    private final LobbyAddon plugin;
    private final DataStorage storage;
    private final LobbyConfig config;

    public NickManager(LobbyAddon p, DataStorage s, LobbyConfig c) {
        this.plugin = p; this.storage = s; this.config = c;
    }

    public String getNick(UUID u) { return storage.getNick(u); }
    public void setNick(UUID u, String n) { storage.setNick(u, n); }
    public void removeNick(UUID u) { storage.removeNick(u); }

    public UUID getUuidByNick(String nick) {
        String test = comparisonKey(nick);
        Map<UUID, String> allNicks = storage.getAllNicks();
        if (allNicks == null) return null;
        for (Map.Entry<UUID, String> e : allNicks.entrySet())
            if (comparisonKey(e.getValue()).equals(test)) return e.getKey();
        return null;
    }

    public String colorize(String t) {
        if (t == null) return "";
        if (!config.colorEnabled) {
            return t.replaceAll("(?i)[&§][0-9a-fk-or]", "")
                    .replaceAll("(?i)[&§]#[0-9a-f]{6}", "");
        }
        if (config.hexEnabled && BukkitVersion.supportsHexColors(plugin.getServer().getBukkitVersion())) {
            Pattern hexPattern = Pattern.compile("(?i)" + Pattern.quote(String.valueOf(config.colorCode))
                    + "#([0-9a-fA-F]{6})");
            Matcher m = hexPattern.matcher(t);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                StringBuilder h = new StringBuilder("\u00a7x");
                for (char ch : m.group(1).toCharArray()) h.append('\u00a7').append(ch);
                m.appendReplacement(sb, Matcher.quoteReplacement(h.toString()));
            }
            m.appendTail(sb); t = sb.toString();
        } else {
            t = t.replaceAll("(?i)" + Pattern.quote(String.valueOf(config.colorCode)) + "#[0-9a-f]{6}", "");
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes(config.colorCode, t);
    }

    public String stripColor(String t) {
        if (t == null) return "";
        return org.bukkit.ChatColor.stripColor(
            org.bukkit.ChatColor.translateAlternateColorCodes(config.colorCode,
                t.replaceAll("(?i)" + Pattern.quote(String.valueOf(config.colorCode)) + "#[0-9a-fA-F]{6}", "")));
    }

    public String comparisonKey(String value) {
        return ProfileTextRules.normalizeForComparison(stripColor(colorize(value)));
    }
}
