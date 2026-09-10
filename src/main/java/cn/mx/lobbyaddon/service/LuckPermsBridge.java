package cn.mx.lobbyaddon.service;

import cn.mx.lobbyaddon.LobbyAddon;
import org.bukkit.Bukkit;

/**
 * Writes only the plugin-owned LuckPerms metadata node at a configured priority.
 * The bridge deliberately never issues a broad remove-prefix/remove-suffix command.
 */
public final class LuckPermsBridge {

    private final LobbyAddon plugin;

    public LuckPermsBridge(LobbyAddon plugin) {
        this.plugin = plugin;
    }

    public boolean isAvailable() {
        return plugin.getServer().getPluginManager().getPlugin("LuckPerms") != null;
    }

    public void replacePrefix(String playerName, String previousValue, String nextValue, int priority) {
        replace(playerName, "prefix", previousValue, nextValue, priority);
    }

    public void replaceSuffix(String playerName, String previousValue, String nextValue, int priority) {
        replace(playerName, "suffix", previousValue, nextValue, priority);
    }

    private void replace(String playerName, String type, String previousValue, String nextValue, int priority) {
        if (!isAvailable()) {
            return;
        }
        if (previousValue != null && !previousValue.isEmpty()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName
                    + " meta remove" + type + " " + priority + " " + previousValue);
        }
        if (nextValue != null && !nextValue.isEmpty()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName
                    + " meta set" + type + " " + priority + " " + nextValue);
        }
    }
}
