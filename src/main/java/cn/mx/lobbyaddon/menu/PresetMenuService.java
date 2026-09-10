package cn.mx.lobbyaddon.menu;

import cn.mx.lobbyaddon.LobbyAddon;
import cn.mx.lobbyaddon.profile.ProfileService;
import cn.mx.lobbyaddon.profile.ProfileType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PresetMenuService {

    private final LobbyAddon plugin;
    private final Set<String> warnedEntries = new HashSet<>();

    public PresetMenuService(LobbyAddon plugin) {
        this.plugin = plugin;
    }

    public boolean open(Player player) {
        return open(player, null);
    }

    public boolean open(Player player, ProfileType filterType) {
        if (!plugin.getConfig().getBoolean("presets.enabled", true)) {
            return false;
        }
        int configuredSize = plugin.getConfig().getInt("presets.size", 27);
        int size = normalizeSize(configuredSize);
        if (configuredSize != size) {
            warnOnce("size", "presets.size 必须是 9 到 54 之间的 9 的倍数，当前按 " + size + " 处理");
        }
        String title = color(plugin.getConfig().getString("presets.title", "&8个人样式"));
        PresetMenuHolder holder = new PresetMenuHolder(filterType);
        Inventory inventory = plugin.getServer().createInventory(holder, size, title);
        holder.setInventory(inventory);

        int added = 0;
        ConfigurationSection entries = plugin.getConfig().getConfigurationSection("presets.entries");
        if (entries != null) {
            for (String key : entries.getKeys(false)) {
                if (addEntry(inventory, holder, entries.getConfigurationSection(key), key, size, filterType)) {
                    added++;
                }
            }
        }
        if (filterType != null) {
            int backSlot = findBackSlot(inventory);
            inventory.setItem(backSlot, menuItem(Material.ARROW, "&e返回", Arrays.asList("&7返回" + typeLabel(filterType) + "设置")));
            holder.setBackSlot(backSlot);
        }
        if (added == 0) {
            int emptySlot = findCenterEmptySlot(inventory);
            inventory.setItem(emptySlot, menuItem(
                    MenuMaterial.renamed("COBWEB", "WEB", (short) 0).createItem(), "&7暂无可用预设",
                    Arrays.asList("&8服务器尚未配置此类样式")));
        }
        if (filterType != null) {
            ItemStack filler = menuItem(MenuMaterial.stainedGlassPane((short) 7).createItem(), " ",
                    Collections.emptyList());
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                if (inventory.getItem(slot) == null) {
                    inventory.setItem(slot, filler);
                }
            }
        }
        player.openInventory(inventory);
        return true;
    }

    private boolean addEntry(Inventory inventory, PresetMenuHolder holder, ConfigurationSection entry,
                             String key, int size, ProfileType filterType) {
        if (entry == null) {
            warnOnce(key, "预设 " + key + " 必须是配置区块");
            return false;
        }
        String typeText = entry.getString("type");
        ProfileType type = ProfileType.fromInput(typeText);
        if (type == null) {
            warnOnce(key + ".type", "预设 " + key + " 的 type 无效: " + typeText);
            return false;
        }
        if (filterType != null && type != filterType) {
            return false;
        }
        String value = entry.getString("value");
        if (value == null || value.trim().isEmpty()) {
            warnOnce(key + ".value", "预设 " + key + " 缺少有效的 value");
            return false;
        }
        int slot = entry.getInt("slot", -1);
        if (slot < 0 || slot >= size) {
            warnOnce(key + ".slot", "预设 " + key + " 的 slot 超出菜单范围: " + slot);
            return false;
        }

        String materialName = entry.getString("material", "NAME_TAG");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            warnOnce(key + ".material", "预设 " + key + " 的 material 无效: " + materialName
                    + "，已改用 NAME_TAG");
            material = Material.NAME_TAG;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(entry.getString("name", "&f" + key)));
            List<String> lore = new ArrayList<>();
            for (String line : entry.getStringList("lore")) {
                lore.add(color(line));
            }
            if (lore.isEmpty()) {
                lore.add("§7点击应用此样式");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        String durationText = entry.getString("duration", "");
        long duration = 0L;
        if (durationText != null && !durationText.trim().isEmpty() && !"0".equals(durationText.trim())) {
            duration = ProfileService.parseDurationMillis(durationText);
            if (duration <= 0L) {
                warnOnce(key + ".duration", "预设 " + key + " 的 duration 无效: " + durationText);
                return false;
            }
        }
        inventory.setItem(slot, item);
        holder.put(slot, new PresetMenuHolder.Preset(type, value, duration, entry.getString("permission", "")));
        return true;
    }

    private static int findBackSlot(Inventory inventory) {
        int[] preferred = {inventory.getSize() - 5, inventory.getSize() - 1, inventory.getSize() - 9};
        for (int slot : preferred) {
            if (slot >= 0 && inventory.getItem(slot) == null) {
                return slot;
            }
        }
        for (int slot = inventory.getSize() - 1; slot >= 0; slot--) {
            if (inventory.getItem(slot) == null) {
                return slot;
            }
        }
        return inventory.getSize() - 1;
    }

    private static int findCenterEmptySlot(Inventory inventory) {
        int center = inventory.getSize() / 2;
        if (inventory.getItem(center) == null) {
            return center;
        }
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                return slot;
            }
        }
        return center;
    }

    private static ItemStack menuItem(Material material, String name, List<String> lore) {
        return menuItem(new ItemStack(material), name, lore);
    }

    private static ItemStack menuItem(ItemStack item, String name, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(color(line));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String typeLabel(ProfileType type) {
        if (type == ProfileType.NICK) return "昵称";
        return type == ProfileType.PREFIX ? "称号" : "后缀";
    }

    private void warnOnce(String key, String message) {
        if (warnedEntries.add(key)) {
            plugin.getLogger().warning(message);
        }
    }

    private static int normalizeSize(int size) {
        int normalized = Math.max(9, Math.min(54, size));
        return ((normalized + 8) / 9) * 9;
    }

    private static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }
}
