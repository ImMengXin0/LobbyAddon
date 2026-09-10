package cn.mx.lobbyaddon.placeholder;

import cn.mx.lobbyaddon.LobbyAddon;
import cn.mx.lobbyaddon.profile.ProfileService;
import cn.mx.lobbyaddon.profile.ProfileType;
import cn.mx.lobbyaddon.util.BukkitCompat;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class LobbyPapiExpansion extends PlaceholderExpansion {
    private final LobbyAddon plugin;
    private final String identifier;

    public LobbyPapiExpansion(LobbyAddon plugin, String identifier) {
        this.plugin = plugin;
        this.identifier = identifier;
    }

    @Override public String getIdentifier() { return identifier; }
    @Override public String getAuthor() { return "ImMengXin"; }
    @Override public String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player pl, String ident) {
        if (ident == null) return null;
        String lower = ident.toLowerCase(java.util.Locale.ROOT);
        if (lower.equals("online")) {
            return String.valueOf(getOnline());
        }
        if (lower.equals("max_online")) {
            return String.valueOf(plugin.getServer().getMaxPlayers());
        }
        if (lower.startsWith("online_")) {
            String server = lower.substring("online_".length());
            return String.valueOf(plugin.getBungeeCounter() != null
                    ? plugin.getBungeeCounter().getServerCount(server) : -1);
        }
        if (lower.equals("bungee_status")) {
            return plugin.getBungeeCounter() == null ? "disabled"
                    : plugin.getBungeeCounter().isFresh() ? "online" : "stale";
        }
        if (pl == null) return "";
        ProfileService profiles = plugin.getProfileService();
        if (profiles == null || plugin.getNickManager() == null) return "";
        switch (lower) {
            case "name": case "nick": {
                String n = profiles.isAllowed(pl, ProfileType.NICK)
                        ? profiles.getEffectiveNick(pl.getUniqueId()) : null;
                return n != null ? plugin.getNickManager().colorize(n) : pl.getName();
            }
            case "realname":
                return pl.getName();
            case "hasnick":
                return profiles.isAllowed(pl, ProfileType.NICK)
                        && profiles.getEffectiveNick(pl.getUniqueId()) != null ? "true" : "false";
            case "display":
                return !isDisplayEnabled() || plugin.getDisplayService() == null ? pl.getName()
                        : plugin.getDisplayService().getDisplayName(pl);
            case "prefix":
                return !isDisplayEnabled() || plugin.getDisplayService() == null ? "" : plugin.getDisplayService().getPrefix(pl);
            case "suffix":
                return !isDisplayEnabled() || plugin.getDisplayService() == null ? "" : plugin.getDisplayService().getSuffix(pl);
            case "hasprefix":
                return isDisplayEnabled() && plugin.getDisplayService() != null
                        && !plugin.getDisplayService().getPrefix(pl).isEmpty() ? "true" : "false";
            case "hassuffix":
                return isDisplayEnabled() && plugin.getDisplayService() != null
                        && !plugin.getDisplayService().getSuffix(pl).isEmpty() ? "true" : "false";
            case "nick_temporary":
                return profiles.hasTemporary(cn.mx.lobbyaddon.profile.ProfileType.NICK, pl.getUniqueId()) ? "true" : "false";
            case "prefix_temporary":
                return profiles.hasTemporary(cn.mx.lobbyaddon.profile.ProfileType.PREFIX, pl.getUniqueId()) ? "true" : "false";
            case "suffix_temporary":
                return profiles.hasTemporary(cn.mx.lobbyaddon.profile.ProfileType.SUFFIX, pl.getUniqueId()) ? "true" : "false";
            case "nick_expires_at":
                return String.valueOf(profiles.getExpiry(cn.mx.lobbyaddon.profile.ProfileType.NICK, pl.getUniqueId()));
            case "prefix_expires_at":
                return String.valueOf(profiles.getExpiry(cn.mx.lobbyaddon.profile.ProfileType.PREFIX, pl.getUniqueId()));
            case "suffix_expires_at":
                return String.valueOf(profiles.getExpiry(cn.mx.lobbyaddon.profile.ProfileType.SUFFIX, pl.getUniqueId()));
            case "nick_cooldown":
                return String.valueOf((profiles.getCooldownRemaining(pl.getUniqueId(),
                        cn.mx.lobbyaddon.profile.ProfileType.NICK) + 999L) / 1000L);
            case "prefix_cooldown":
                return String.valueOf((profiles.getCooldownRemaining(pl.getUniqueId(),
                        cn.mx.lobbyaddon.profile.ProfileType.PREFIX) + 999L) / 1000L);
            case "suffix_cooldown":
                return String.valueOf((profiles.getCooldownRemaining(pl.getUniqueId(),
                        cn.mx.lobbyaddon.profile.ProfileType.SUFFIX) + 999L) / 1000L);
            default:
                return null;
        }
    }

    private int getOnline() {
        if (plugin.getBungeeCounter() != null && plugin.getConfig().getBoolean("bungee.enabled", true)) {
            return plugin.getBungeeCounter().getTotal();
        }
        return BukkitCompat.getOnlinePlayerCount();
    }

    private boolean isDisplayEnabled() {
        return plugin.getConfig().getBoolean("display.enabled", true);
    }
}
