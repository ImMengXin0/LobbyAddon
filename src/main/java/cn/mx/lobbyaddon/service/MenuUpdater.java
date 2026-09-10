package cn.mx.lobbyaddon.service;

import cn.mx.lobbyaddon.LobbyAddon;
import cn.mx.lobbyaddon.util.BukkitCompat;
import cn.mx.lobbyaddon.util.MenuAmountRules;
import cn.mx.lobbyaddon.util.MenuPlaceholder;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 动态菜单数量更新器
 * 兼容 DeluxeMenus 等第三方菜单插件：
 * 在菜单物品的 lore 里写 %lobbyaddon_online%（或 %lobbyaddon_max_online%），
 * 本插件每秒把该物品的 amount 更新为占位符解析出的数字。
 * 例:
 *   item:
 *     material: DIAMOND
 *     name: "&b在线玩家"
 *     lore:
 *       - "&7当前: %lobbyaddon_online% 人"
 *     amount: 1
 * 效果: 物品数量实时等于在线人数。
 */
public class MenuUpdater {

    private static final Pattern MARKER = Pattern.compile(
            "%(lobbyaddon|nick)_(online|max_online)(_[a-zA-Z0-9_-]+)?%", Pattern.CASE_INSENSITIVE);

    private final LobbyAddon plugin;
    private BukkitTask task;
    private final java.util.Map<java.util.UUID, Long> lastWarn = new java.util.HashMap<>();

    /** 每 30 秒最多对同一玩家告警一次，避免刷屏 */
    private void warnOnce(Player player, String msg) {
        long now = System.currentTimeMillis();
        Long last = lastWarn.get(player.getUniqueId());
        if (last != null && now - last < 30000) return;
        lastWarn.put(player.getUniqueId(), now);
        plugin.getLogger().warning(msg + " (玩家: " + player.getName() + ")");
    }

    public MenuUpdater(LobbyAddon plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (task != null) return;
        int interval = Math.max(1, plugin.getConfig().getInt("menu-updater.interval", 1)) * 20;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        lastWarn.clear();
    }

    private void updateAll() {
        if (!plugin.getConfig().getBoolean("menu-updater.enabled", true)) return;
        for (Player p : BukkitCompat.getOnlinePlayers()) {
            try {
                updateFor(p);
            } catch (Exception exception) {
                warnOnce(p, "动态菜单更新异常: " + exception.getClass().getSimpleName()
                        + (exception.getMessage() == null ? "" : " - " + exception.getMessage()));
            }
        }
    }

    private void updateFor(Player player) {
        Inventory inv = player.getOpenInventory() == null ? null
                : player.getOpenInventory().getTopInventory();
        if (inv == null) return;

        Object holder = inv.getHolder();
        // DeluxeMenus 1.13.x 部分版本创建菜单时不带 holder（null），
        // 原版容器一定有 holder（BlockState/动物等），所以 holder==null 视为菜单
        boolean isMenu;
        if (holder == null) {
            isMenu = plugin.getConfig().getBoolean("menu-updater.allow-null-holder", false);
        } else {
            String holderClass = holder.getClass().getName().toLowerCase();
            isMenu = holderClass.contains("deluxemenus");
        }
        if (!isMenu) return;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null) continue;

            String marker = findMarker(item);
            if (marker == null) continue;

            String parsed = parsePlaceholder(player, marker);
            int value = MenuAmountRules.toDisplayAmount(parseIntSafe(parsed), item.getMaxStackSize());
            if (value < 0) {
                warnOnce(player, "动态数量占位符 [" + marker + "] 解析失败（子服名是否在 config.yml 的 bungee.servers 中？代理是否响应？）");
                value = 1;
            }

            if (item.getAmount() != value) {
                item.setAmount(value);
                inv.setItem(i, item);
                if (plugin.getConfig().getBoolean("debug.menu-updates", false)) {
                    plugin.getLogger().info("动态数量已更新 " + player.getName()
                            + " 物品 [" + marker + "] → " + value);
                }
            }
        }
    }

    /** 在物品的 name/lore 中查找动态数量占位符 */
    private String findMarker(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        if (meta.hasDisplayName()) {
            Matcher m = MARKER.matcher(meta.getDisplayName());
            if (m.find()) return m.group(0);
        }
        if (meta.hasLore()) {
            for (String line : meta.getLore()) {
                Matcher m = MARKER.matcher(line);
                if (m.find()) return m.group(0);
            }
        }
        return null;
    }

    private String parsePlaceholder(Player player, String raw) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                String resolved = PlaceholderAPI.setPlaceholders(player, raw);
                if (resolved != null && !resolved.equals(raw)) {
                    return resolved;
                }
            } catch (Throwable ignored) {
            }
        }
        String server = MenuPlaceholder.onlineServer(raw);
        if (server != null) {
            if (plugin.getBungeeCounter() != null) {
                int count = plugin.getBungeeCounter().getServerCount(server);
                return count >= 0 ? String.valueOf(count) : raw;
            }
            return raw;
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.equals("%lobbyaddon_online%") || lower.equals("%nick_online%")) {
            if (plugin.getBungeeCounter() != null) {
                return String.valueOf(plugin.getBungeeCounter().getTotal());
            }
            return String.valueOf(BukkitCompat.getOnlinePlayerCount());
        }
        if (lower.equals("%lobbyaddon_max_online%") || lower.equals("%nick_max_online%")) {
            return String.valueOf(plugin.getServer().getMaxPlayers());
        }
        return raw;
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
