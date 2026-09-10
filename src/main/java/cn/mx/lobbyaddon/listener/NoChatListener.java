package cn.mx.lobbyaddon.listener;

import cn.mx.lobbyaddon.LobbyAddon;
import cn.mx.lobbyaddon.config.LobbyConfig;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NoChat 禁聊功能（整合自原 NoChat 插件）
 * 在登录服/大厅服禁用所有聊天，除非玩家有绕过权限。
 */
public class NoChatListener implements Listener {

    private final LobbyAddon plugin;
    private final LobbyConfig config;
    private final Map<UUID, Long> lastNotice = new ConcurrentHashMap<>();

    public NoChatListener(LobbyAddon plugin, LobbyConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (plugin.getPersonalMenuService() != null
                && plugin.getPersonalMenuService().hasPending(event.getPlayer().getUniqueId())) return;
        if (!isRestricted(event.getPlayer())) return;

        event.setCancelled(true);
        sendNotice(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfig().getBoolean("nochat.block-commands", false) || !isRestricted(event.getPlayer())) return;
        String command = event.getMessage().split("\\s+", 2)[0].toLowerCase();
        for (String allowed : plugin.getConfig().getStringList("nochat.allowed-commands")) {
            String normalized = allowed.startsWith("/") ? allowed.toLowerCase() : "/" + allowed.toLowerCase();
            if (command.equals(normalized)) return;
        }
        event.setCancelled(true);
        sendNotice(event.getPlayer());
    }
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastNotice.remove(event.getPlayer().getUniqueId());
    }


    private boolean isRestricted(org.bukkit.entity.Player player) {
        if (!config.nochatEnabled || player.hasPermission(config.nochatBypassPerm)) return false;
        for (String world : plugin.getConfig().getStringList("nochat.exempt-worlds")) {
            if (world.equalsIgnoreCase(player.getWorld().getName())) return false;
        }
        return true;
    }

    private void sendNotice(org.bukkit.entity.Player player) {
        long cooldown = Math.max(0L, plugin.getConfig().getLong("nochat.message-cooldown-seconds", 3L)) * 1000L;
        long now = System.currentTimeMillis();
        Long previous = lastNotice.put(player.getUniqueId(), now);
        if (previous != null && now - previous < cooldown) return;
        if (config.nochatMessage != null && !config.nochatMessage.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.nochatMessage));
        }
    }
}
