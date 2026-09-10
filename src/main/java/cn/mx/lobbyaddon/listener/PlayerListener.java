package cn.mx.lobbyaddon.listener;

import cn.mx.lobbyaddon.LobbyAddon;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.ChatColor;

import java.util.concurrent.ExecutionException;

public class PlayerListener implements Listener {

    private static final String DISPLAY_TOKEN = "\u0000lobbyaddon-display\u0000";
    private static final String MESSAGE_TOKEN = "\u0000lobbyaddon-message\u0000";

    private final LobbyAddon plugin;

    public PlayerListener(LobbyAddon plugin) {
        this.plugin = plugin;
    }

    private String resolvePlaceholders(final Player player, final String configured, boolean asynchronous) {
        if (configured == null) {
            return "{display} &8> &f{message}";
        }
        if (configured.indexOf('%') < 0
                || Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return configured;
        }
        if (!asynchronous) {
            return applyPlaceholders(player, configured);
        }
        try {
            return Bukkit.getScheduler().callSyncMethod(plugin,
                    () -> applyPlaceholders(player, configured)).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | RuntimeException ignored) {
        }
        return configured;
    }

    private String applyPlaceholders(Player player, String input) {
        try {
            String resolved = PlaceholderAPI.setPlaceholders(player, input);
            return resolved == null ? input : resolved;
        } catch (Throwable ignored) {
            return input;
        }
    }

    private static String escapeFormatPercents(String format) {
        return format.replace("%1$s", DISPLAY_TOKEN)
                .replace("%2$s", MESSAGE_TOKEN)
                .replace("%", "%%")
                .replace(DISPLAY_TOKEN, "%1$s")
                .replace(MESSAGE_TOKEN, "%2$s");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent e) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (plugin.getDisplayService() != null) {
                plugin.getDisplayService().refreshViewer(e.getPlayer());
            }
        });
        if (plugin.getConfig().getBoolean("display.enabled", true)
                && e.getJoinMessage() != null && plugin.getDisplayService() != null) {
            e.setJoinMessage(e.getJoinMessage().replace(e.getPlayer().getName(),
                    plugin.getDisplayService().getDisplayName(e.getPlayer())));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        if (plugin.getPersonalMenuService() != null
                && plugin.getPersonalMenuService().hasPending(e.getPlayer().getUniqueId())) return;
        if (plugin.getConfig().getBoolean("chat-color.enabled", true)
                && plugin.getChatColorService() != null) {
            e.setMessage(plugin.getChatColorService().render(e.getPlayer(), e.getMessage()));
        }
        if (!plugin.getConfig().getBoolean("display.enabled", true)
                || !plugin.getConfig().getBoolean("display.chat.enabled", true)) return;
        String configured = plugin.getConfig().getString("display.chat.format", "{display} &8» &f{message}");
        String format = ChatColor.translateAlternateColorCodes('&',
                resolvePlaceholders(e.getPlayer(), configured, e.isAsynchronous()))
                .replace("{display}", DISPLAY_TOKEN).replace("{message}", MESSAGE_TOKEN);
        if (!format.contains(DISPLAY_TOKEN) || !format.contains(MESSAGE_TOKEN)) {
            format = DISPLAY_TOKEN + " \u00a78> \u00a7f" + MESSAGE_TOKEN;
        }
        String baseName = plugin.getDisplayService() == null ? e.getPlayer().getName()
                : plugin.getDisplayService().getBaseName(e.getPlayer());
        format = format.replace(DISPLAY_TOKEN, baseName).replace(MESSAGE_TOKEN, "%2$s");
        e.setFormat(escapeFormatPercents(format));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDeath(PlayerDeathEvent e) {
        if (plugin.getConfig().getBoolean("display.enabled", true)
                && e.getDeathMessage() != null && plugin.getDisplayService() != null) {
            e.setDeathMessage(e.getDeathMessage().replace(e.getEntity().getName(),
                    plugin.getDisplayService().getDisplayName(e.getEntity())));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onRespawn(PlayerRespawnEvent e) {
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.refreshDisplay(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.refreshDisplay(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.getDisplayService() != null) {
            plugin.getDisplayService().clearNametag(event.getPlayer());
        }
    }
}
