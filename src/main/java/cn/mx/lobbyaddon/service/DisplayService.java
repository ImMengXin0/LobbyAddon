package cn.mx.lobbyaddon.service;

import cn.mx.lobbyaddon.LobbyAddon;
import cn.mx.lobbyaddon.profile.ProfileService;
import cn.mx.lobbyaddon.profile.ProfileType;
import cn.mx.lobbyaddon.util.BukkitCompat;
import cn.mx.lobbyaddon.util.LegacyTextLimiter;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Keeps chat, player list and optional scoreboard nametags on one profile representation. */
public final class DisplayService {

    private final LobbyAddon plugin;
    private final NickManager nickManager;
    private final ProfileService profiles;

    public DisplayService(LobbyAddon plugin, NickManager nickManager, ProfileService profiles) {
        this.plugin = plugin;
        this.nickManager = nickManager;
        this.profiles = profiles;
    }

    public String getDisplayName(Player player) {
        return getPrefix(player) + getBaseName(player) + getSuffix(player);
    }

    /** Returns only the player's effective nickname or real name, without cosmetic prefix or suffix. */
    public String getBaseName(Player player) {
        String nick = profiles.isAllowed(player, ProfileType.NICK)
                ? profiles.getEffectiveNick(player.getUniqueId()) : null;
        return nick == null ? player.getName() : nickManager.colorize(nick);
    }

    public String getPrefix(Player player) {
        if (!profiles.isAllowed(player, ProfileType.PREFIX)) {
            return "";
        }
        String value = profiles.getEffectivePrefix(player.getUniqueId());
        if (value == null || value.isEmpty()) {
            value = resolveGroupValue(player, "prefix");
        }
        if (value == null || value.isEmpty()) {
            return "";
        }
        String colored = nickManager.colorize(value);
        return colored.endsWith(" ") ? colored : colored + " ";
    }

    public String getSuffix(Player player) {
        if (!profiles.isAllowed(player, ProfileType.SUFFIX)) {
            return "";
        }
        String custom = profiles.getEffectiveSuffix(player.getUniqueId());
        if (custom != null && !custom.isEmpty()) {
            return profiles.formatSuffix(custom);
        }
        String group = resolveGroupValue(player, "suffix");
        return group == null || group.isEmpty() ? "" : nickManager.colorize(group);
    }

    public void refresh(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (!plugin.getConfig().getBoolean("display.enabled", true)) {
            clear(player);
            return;
        }
        String display = getDisplayName(player);
        player.setDisplayName(display);
        if (plugin.getConfig().getBoolean("display.tab-list.enabled", true)) {
            player.setPlayerListName(limitPlain(display, 16));
        } else {
            player.setPlayerListName(player.getName());
        }
        if (plugin.getConfig().getBoolean("display.nametag.enabled", false)) {
            refreshNametag(player);
        } else {
            clearNametag(player);
        }
    }

    public void refreshAll() {
        for (Player player : BukkitCompat.getOnlinePlayers()) {
            refresh(player);
        }
    }

    public void refreshViewer(Player viewer) {
        refresh(viewer);
        if (!plugin.getConfig().getBoolean("display.enabled", true)
                || !plugin.getConfig().getBoolean("display.nametag.enabled", false)) {
            return;
        }
        for (Player player : BukkitCompat.getOnlinePlayers()) {
            refreshNametagForViewer(player, viewer);
        }
    }

    public void clearAll() {
        for (Player player : BukkitCompat.getOnlinePlayers()) {
            clear(player);
        }
    }

    private void clear(Player player) {
        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());
        clearNametag(player);
    }

    public void clearNametag(Player player) {
        if (player == null) {
            return;
        }
        for (Player viewer : BukkitCompat.getOnlinePlayers()) {
            Scoreboard board = viewer.getScoreboard();
            Team team = board.getTeam(teamName(player.getUniqueId()));
            if (team != null) {
                team.removeEntry(player.getName());
                if (team.getEntries().isEmpty()) {
                    team.unregister();
                }
            }
        }
    }

    private void refreshNametag(Player player) {
        for (Player viewer : BukkitCompat.getOnlinePlayers()) {
            refreshNametagForViewer(player, viewer);
        }
    }

    private void refreshNametagForViewer(Player player, Player viewer) {
        Scoreboard board = viewer.getScoreboard();
        String teamName = teamName(player.getUniqueId());
        Team existing = board.getEntryTeam(player.getName());
        if (existing != null && !teamName.equals(existing.getName())) {
            return;
        }
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }
        team.addEntry(player.getName());
        try {
            team.setPrefix(limitFormatted(getPrefix(player), 16));
            team.setSuffix(limitFormatted(getSuffix(player), 16));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Unable to update nametag for " + player.getName() + ": "
                    + exception.getMessage());
        }
    }

    private String resolveGroupValue(Player player, String field) {
        List<Map<?, ?>> defaults = plugin.getConfig().getMapList("group-defaults");
        for (Map<?, ?> entry : defaults) {
            String permission = stringValue(entry.get("permission"));
            if (permission == null || permission.isEmpty() || !player.hasPermission(permission)) {
                continue;
            }
            if (!matchesContext(entry.get("worlds"), player.getWorld().getName())) {
                continue;
            }
            if (!matchesContext(entry.get("servers"), BukkitCompat.getServerName(plugin))) {
                continue;
            }
            return stringValue(entry.get(field));
        }
        return null;
    }

    private static boolean matchesContext(Object configured, String current) {
        if (configured == null) {
            return true;
        }
        if (configured instanceof List) {
            List<?> values = (List<?>) configured;
            if (values.isEmpty()) {
                return true;
            }
            for (Object value : values) {
                if (current.equalsIgnoreCase(String.valueOf(value))) {
                    return true;
                }
            }
            return false;
        }
        String value = String.valueOf(configured);
        return value.isEmpty() || current.equalsIgnoreCase(value);
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String limitPlain(String value, int limit) {
        String plain = nickManager.stripColor(value);
        return plain.length() <= limit ? plain : plain.substring(0, limit);
    }

    private static String limitFormatted(String value, int limit) {
        return LegacyTextLimiter.limitSerialized(value, limit);
    }

    private static String teamName(UUID uuid) {
        return "la" + uuid.toString().replace("-", "").substring(0, 14);
    }
}
