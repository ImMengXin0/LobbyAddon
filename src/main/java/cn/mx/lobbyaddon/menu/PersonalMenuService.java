package cn.mx.lobbyaddon.menu;

import cn.mx.lobbyaddon.LobbyAddon;
import cn.mx.lobbyaddon.chat.ChatColorParser;
import cn.mx.lobbyaddon.chat.ChatFormat;
import cn.mx.lobbyaddon.chat.ChatStyle;
import cn.mx.lobbyaddon.chat.ChatStyleCodec;
import cn.mx.lobbyaddon.command.CommandMessages;
import cn.mx.lobbyaddon.profile.ProfileService;
import cn.mx.lobbyaddon.profile.ProfileType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static cn.mx.lobbyaddon.menu.PersonalMenuHolder.Action;
import static cn.mx.lobbyaddon.menu.PersonalMenuHolder.ActionType;

public final class PersonalMenuService {

    private static final long MINUTES_30 = 30L * 60L * 1000L;
    private static final long HOUR_1 = 60L * 60L * 1000L;
    private static final long DAY_1 = 24L * 60L * 60L * 1000L;
    private static final int[] LEGACY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16, 17,
            19, 20, 21, 22, 23, 24, 25, 26
    };
    private static final int[] PRESET_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final String[] COLOR_NAMES = {
            "黑色", "深蓝", "深绿", "深青", "深红", "紫色", "金色", "灰色",
            "深灰", "蓝色", "亮绿", "青色", "红色", "粉色", "黄色", "白色"
    };
    private static final short[] WOOL_DATA = {
            15, 11, 13, 9, 14, 10, 1, 8, 7, 3, 5, 9, 14, 2, 4, 0
    };

    private final LobbyAddon plugin;
    private final Map<UUID, PendingInput> pendingInputs = new ConcurrentHashMap<>();
    private final Set<UUID> forcedSelfNickCommands = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());

    public PersonalMenuService(LobbyAddon plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player player) {
        PersonalMenuHolder holder = holder(PersonalMenuHolder.Page.MAIN, null, 0, 27,
                configTitle("personal-menu.title", "&8个人设置"));
        Inventory inventory = holder.getInventory();
        decorate(inventory, (short) 7, (short) 9);

        inventory.setItem(4, playerSummary(player));
        set(inventory, holder, 10, featureItem(player, ProfileType.NICK, Material.NAME_TAG,
                "&b昵称设置", "更改聊天与展示昵称"), Action.profile(ActionType.OPEN_PROFILE, ProfileType.NICK));
        set(inventory, holder, 12, featureItem(player, ProfileType.PREFIX, Material.GOLD_INGOT,
                "&6自定义称号", "设置名字前方的个性称号"), Action.profile(ActionType.OPEN_PROFILE, ProfileType.PREFIX));
        set(inventory, holder, 14, featureItem(player, ProfileType.SUFFIX, Material.IRON_INGOT,
                "&f自定义后缀", "设置名字后方的括号后缀"), Action.profile(ActionType.OPEN_PROFILE, ProfileType.SUFFIX));
        set(inventory, holder, 16, chatFeatureItem(player), Action.simple(ActionType.OPEN_CHAT));
        set(inventory, holder, 22, item(Material.BARRIER, 0, "&c关闭菜单",
                "&7点击关闭个人设置"), Action.simple(ActionType.CLOSE));
        player.openInventory(inventory);
    }

    public void openProfile(Player player, ProfileType type) {
        String label = profileLabel(type);
        PersonalMenuHolder holder = holder(PersonalMenuHolder.Page.PROFILE, type, 0, 27,
                configTitle("personal-menu.profile-title", "&8设置 - {feature}")
                        .replace("{feature}", label));
        Inventory inventory = holder.getInventory();
        decorate(inventory, (short) 7, profileAccent(type));

        inventory.setItem(4, profileSummary(player, type));
        set(inventory, holder, 10, item(Material.NAME_TAG, 0, "&a永久设置",
                "&7在聊天中输入新的" + label,
                "&8永久生效，直到再次修改",
                clickStatus(canUseProfile(player, type))), Action.profileInput(type, 0L));
        addDuration(inventory, holder, player, type, 11, MINUTES_30, "30 分钟");
        addDuration(inventory, holder, player, type, 12, HOUR_1, "1 小时");
        addDuration(inventory, holder, player, type, 13, DAY_1, "1 天");
        set(inventory, holder, 15, item(Material.REDSTONE_BLOCK, 0, "&c重置" + label,
                "&7恢复服务器默认显示",
                clickStatus(canUseProfile(player, type))), Action.profile(ActionType.PROFILE_RESET, type));

        boolean presetsEnabled = plugin.getConfig().getBoolean("presets.enabled", true);
        set(inventory, holder, 16, item(presetsEnabled ? Material.CHEST : Material.TRAPPED_CHEST, 0,
                presetsEnabled ? "&d选择样式预设" : "&c样式预设未启用",
                presetsEnabled ? "&7查看可用的" + label + "预设" : "&7请联系服务器管理员",
                presetsEnabled ? "&e点击打开" : "&8当前不可用"),
                Action.profile(ActionType.PROFILE_PRESETS, type));
        set(inventory, holder, 22, item(Material.ARROW, 0, "&e返回",
                "&7返回个人设置"), Action.simple(ActionType.BACK_MAIN));
        player.openInventory(inventory);
    }

    public void openChat(Player player) {
        PersonalMenuHolder holder = holder(PersonalMenuHolder.Page.CHAT, null, 0, 54,
                configTitle("personal-menu.chat-title", "&8聊天颜色与格式"));
        Inventory inventory = holder.getInventory();
        fill(inventory, (short) 7);
        frameRows(inventory, (short) 9);

        ChatStyle current = plugin.getChatColorService().getStyle(player.getUniqueId());
        inventory.setItem(4, item(Material.PAPER, 0, "&b当前聊天样式",
                "&7" + describeStyle(current),
                "&f" + plugin.getChatColorService().render(player,
                        plugin.getConfig().getString("chat-color.preview-text", "这是一条聊天颜色预览消息"))));

        for (int index = 0; index < 16; index++) {
            char code = "0123456789abcdef".charAt(index);
            boolean allowed = legacyAllowed(code) && hasChatPermission(player, "legacy", "lobbyaddon.chatcolor.legacy")
                    && plugin.getConfig().getBoolean("chat-color.legacy.enabled", true);
            String selected = current.getMode() == ChatStyle.Mode.LEGACY && current.getLegacyColor() == code
                    ? "&a当前正在使用" : clickStatus(allowed);
            set(inventory, holder, LEGACY_SLOTS[index],
                    item(MenuMaterial.wool(WOOL_DATA[index]), "&" + code + COLOR_NAMES[index],
                            "&7传统聊天颜色 &" + code, selected),
                    Action.value(ActionType.CHAT_LEGACY, String.valueOf(code)));
        }

        boolean hexAllowed = modernEnabled("hex-enabled")
                && hasChatPermission(player, "hex", "lobbyaddon.chatcolor.hex");
        boolean gradientAllowed = modernEnabled("gradient-enabled")
                && hasChatPermission(player, "gradient", "lobbyaddon.chatcolor.gradient");
        set(inventory, holder, 29, item(MenuMaterial.renamed("PURPLE_DYE", "INK_SACK", (short) 5), "&dHex 单色",
                "&7输入一个六位颜色值",
                "&8示例: #12ABEF", clickStatus(hexAllowed)), Action.simple(ActionType.CHAT_HEX_INPUT));
        set(inventory, holder, 30, item(MenuMaterial.renamed("LIGHT_BLUE_DYE", "INK_SACK", (short) 12), "&6渐变颜色",
                "&7输入起始色与结束色",
                "&8示例: #FF512F #F09819", clickStatus(gradientAllowed)),
                Action.simple(ActionType.CHAT_GRADIENT_INPUT));
        set(inventory, holder, 31, item(Material.NETHER_STAR, 0, "&e颜色组合预设",
                "&7使用服务器配置的颜色与格式组合",
                clickStatus(hasChatPermission(player, "preset", "lobbyaddon.chatcolor.preset"))),
                Action.simple(ActionType.OPEN_CHAT_PRESETS));
        set(inventory, holder, 32, item(MenuMaterial.renamed("ENDER_EYE", "EYE_OF_ENDER", (short) 0), "&b预览当前样式",
                "&7在聊天栏查看效果", "&e点击预览"), Action.simple(ActionType.CHAT_PREVIEW));
        set(inventory, holder, 33, item(Material.REDSTONE_BLOCK, 0, "&c重置聊天样式",
                "&7清除颜色与所有文字格式", "&e点击重置"), Action.simple(ActionType.CHAT_RESET));

        addFormat(inventory, holder, player, current, 37, ChatFormat.BOLD, Material.DIAMOND, "加粗");
        addFormat(inventory, holder, player, current, 38, ChatFormat.ITALIC, Material.FEATHER, "斜体");
        addFormat(inventory, holder, player, current, 39, ChatFormat.UNDERLINE, Material.STRING, "下划线");
        addFormat(inventory, holder, player, current, 40, ChatFormat.STRIKETHROUGH, Material.IRON_SWORD, "删除线");
        addFormat(inventory, holder, player, current, 41, ChatFormat.OBFUSCATED, Material.ENDER_PEARL, "乱码");

        set(inventory, holder, 49, item(Material.ARROW, 0, "&e返回",
                "&7返回个人设置"), Action.simple(ActionType.BACK_MAIN));
        set(inventory, holder, 53, item(Material.BARRIER, 0, "&c关闭菜单",
                "&7点击关闭"), Action.simple(ActionType.CLOSE));
        player.openInventory(inventory);
    }

    public void openChatPresets(Player player, int requestedPage) {
        List<String> names = plugin.getChatColorService().getPresetNames();
        int pageCount = Math.max(1, (names.size() + PRESET_SLOTS.length - 1) / PRESET_SLOTS.length);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        PersonalMenuHolder holder = holder(PersonalMenuHolder.Page.CHAT_PRESETS, null, page, 54,
                configTitle("personal-menu.chat-presets-title", "&8聊天颜色预设"));
        Inventory inventory = holder.getInventory();
        fill(inventory, (short) 7);
        frameRows(inventory, (short) 4);

        int start = page * PRESET_SLOTS.length;
        int end = Math.min(names.size(), start + PRESET_SLOTS.length);
        for (int index = start; index < end; index++) {
            String name = names.get(index);
            ChatStyle style = plugin.getChatColorService().getPreset(name);
            boolean allowed = plugin.getChatColorService().isPresetAllowed(player, name)
                    && hasChatPermission(player, "preset", "lobbyaddon.chatcolor.preset");
            set(inventory, holder, PRESET_SLOTS[index - start], item(
                    MenuMaterial.renamed("FIREWORK_STAR", "FIREWORK_CHARGE", (short) 0),
                    allowed ? "&e" + name : "&c" + name,
                    "&7" + describeStyle(style), clickStatus(allowed)),
                    Action.value(ActionType.CHAT_PRESET, name));
        }
        if (names.isEmpty()) {
            inventory.setItem(22, item(MenuMaterial.renamed("COBWEB", "WEB", (short) 0), "&7暂无聊天颜色预设",
                    "&8可在 config.yml 的 chat-color.presets 中添加"));
        }
        if (page > 0) {
            set(inventory, holder, 45, item(Material.ARROW, 0, "&e上一页",
                    "&7第 " + page + " / " + pageCount + " 页"), Action.simple(ActionType.PREVIOUS_PAGE));
        }
        set(inventory, holder, 49, item(Material.ARROW, 0, "&e返回聊天颜色",
                "&7返回颜色与格式设置"), Action.simple(ActionType.BACK_CHAT));
        if (page + 1 < pageCount) {
            set(inventory, holder, 53, item(Material.ARROW, 0, "&e下一页",
                    "&7第 " + (page + 2) + " / " + pageCount + " 页"), Action.simple(ActionType.NEXT_PAGE));
        }
        player.openInventory(inventory);
    }

    public void startProfileInput(Player player, ProfileType type, long durationMillis) {
        String duration = durationMillis <= 0L ? "永久" : ProfileService.formatDuration(durationMillis);
        beginInput(player, new PendingInput(InputKind.PROFILE, type, durationMillis),
                "请在聊天中输入新的" + profileLabel(type) + "（" + duration + "）。");
    }

    public void startHexInput(Player player) {
        beginInput(player, new PendingInput(InputKind.HEX, null, 0L),
                "请在聊天中输入六位 Hex 颜色，例如 #12ABEF。");
    }

    public void startGradientInput(Player player) {
        beginInput(player, new PendingInput(InputKind.GRADIENT, null, 0L),
                "请在聊天中输入两个 Hex 颜色，例如 #FF512F #F09819。");
    }

    public boolean hasPending(UUID uuid) {
        return pendingInputs.containsKey(uuid);
    }

    public PendingInput getPending(UUID uuid) {
        PendingInput input = pendingInputs.get(uuid);
        if (input != null && input.expiresAt <= System.currentTimeMillis()) {
            pendingInputs.remove(uuid, input);
            return null;
        }
        return input;
    }

    public boolean completePending(UUID uuid, PendingInput input) {
        return pendingInputs.remove(uuid, input);
    }

    public void cancelPending(UUID uuid) {
        pendingInputs.remove(uuid);
    }

    public String commandFor(PendingInput pending, String value) {
        if (pending.kind == InputKind.HEX) {
            return PersonalMenuInput.hexCommand(value);
        }
        if (pending.kind == InputKind.GRADIENT) {
            return PersonalMenuInput.gradientCommand(value);
        }
        return PersonalMenuInput.profileCommand(profileCommand(pending.profileType), pending.durationMillis, value);
    }

    public String invalidHint(PendingInput pending) {
        if (pending.kind == InputKind.HEX) {
            return "请输入六位 Hex 颜色，例如 #12ABEF，或输入 取消。";
        }
        if (pending.kind == InputKind.GRADIENT) {
            return "请输入两个 Hex 颜色，例如 #FF512F #F09819，或输入 取消。";
        }
        return "输入内容无效，请重新输入，或输入 取消。";
    }

    public void performPendingCommand(Player player, PendingInput pending, String command) {
        boolean forceSelf = pending.kind == InputKind.PROFILE && pending.profileType == ProfileType.NICK;
        if (forceSelf) {
            forcedSelfNickCommands.add(player.getUniqueId());
        }
        try {
            player.performCommand(command);
        } finally {
            if (forceSelf) {
                forcedSelfNickCommands.remove(player.getUniqueId());
            }
        }
    }

    public boolean isForcingSelfNick(UUID uuid) {
        return forcedSelfNickCommands.contains(uuid);
    }

    public boolean canUseProfile(Player player, ProfileType type) {
        return isProfileEnabled(type) && player.hasPermission(profilePermission(type))
                && plugin.getProfileService().isAllowed(player, type);
    }

    public boolean canUseProfileDuration(Player player, ProfileType type, long durationMillis) {
        if (!canUseProfile(player, type)) {
            return false;
        }
        if (durationMillis <= 0L) {
            return true;
        }
        long maximum = Math.max(1L,
                plugin.getConfig().getLong("temporary.max-duration-seconds", 604800L)) * 1000L;
        return plugin.getConfig().getBoolean("temporary.enabled", true) && durationMillis <= maximum;
    }

    public boolean isProfileEnabled(ProfileType type) {
        if (type == ProfileType.PREFIX) {
            return plugin.getLobbyConfig().customEnabled;
        }
        return type != ProfileType.SUFFIX || plugin.getLobbyConfig().suffixEnabled;
    }

    public String profilePermission(ProfileType type) {
        if (type == ProfileType.NICK) {
            return plugin.getLobbyConfig().nickUse;
        }
        if (type == ProfileType.PREFIX) {
            return plugin.getLobbyConfig().customUse;
        }
        return plugin.getLobbyConfig().suffixUse;
    }

    public boolean isChatEnabled() {
        return plugin.getConfig().getBoolean("chat-color.enabled", true);
    }

    public boolean canUseChat(Player player) {
        return isChatEnabled() && hasChatPermission(player, "use", "lobbyaddon.chatcolor.use");
    }

    public boolean canUseHex(Player player) {
        return canUseChat(player) && modernEnabled("hex-enabled")
                && hasChatPermission(player, "hex", "lobbyaddon.chatcolor.hex");
    }

    public boolean canUseLegacy(Player player, char color) {
        return canUseChat(player) && plugin.getConfig().getBoolean("chat-color.legacy.enabled", true)
                && legacyAllowed(Character.toLowerCase(color))
                && hasChatPermission(player, "legacy", "lobbyaddon.chatcolor.legacy");
    }

    public boolean canUseGradient(Player player) {
        return canUseChat(player) && modernEnabled("gradient-enabled")
                && hasChatPermission(player, "gradient", "lobbyaddon.chatcolor.gradient");
    }

    public boolean canUseChatPresets(Player player) {
        return canUseChat(player) && hasChatPermission(player, "preset", "lobbyaddon.chatcolor.preset");
    }

    public boolean canUseFormat(Player player, ChatFormat format) {
        return canUseChat(player) && plugin.getConfig().getBoolean("chat-color.formats.enabled", true)
                && formatAllowed(format)
                && hasChatPermission(player, "format", "lobbyaddon.chatcolor.format")
                && (format != ChatFormat.OBFUSCATED
                || hasChatPermission(player, "obfuscated", "lobbyaddon.chatcolor.obfuscated"));
    }

    public boolean hasChatPermission(Player player, String key, String defaultPermission) {
        String permission = plugin.getConfig().getString("chat-color.permissions." + key, defaultPermission);
        return permission == null || permission.trim().isEmpty() || player.hasPermission(permission);
    }

    public void close() {
        pendingInputs.clear();
        forcedSelfNickCommands.clear();
    }

    private void beginInput(Player player, PendingInput pending, String prompt) {
        long timeoutSeconds = Math.max(10L,
                plugin.getConfig().getLong("personal-menu.input-timeout-seconds", 60L));
        pending.expiresAt = System.currentTimeMillis() + timeoutSeconds * 1000L;
        pendingInputs.put(player.getUniqueId(), pending);
        player.closeInventory();
        CommandMessages.info(player, prompt);
        CommandMessages.info(player, "输入 取消 或 cancel 可返回菜单，等待时间 "
                + timeoutSeconds + " 秒。");
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (pendingInputs.remove(player.getUniqueId(), pending) && player.isOnline()) {
                CommandMessages.error(player, "菜单输入已超时，请重新打开菜单。");
            }
        }, timeoutSeconds * 20L);
    }

    private PersonalMenuHolder holder(PersonalMenuHolder.Page page, ProfileType type, int pageIndex,
                                      int size, String title) {
        PersonalMenuHolder holder = new PersonalMenuHolder(page, type, pageIndex);
        Inventory inventory = plugin.getServer().createInventory(holder, size, trimTitle(title));
        holder.setInventory(inventory);
        return holder;
    }

    private ItemStack playerSummary(Player player) {
        ItemStack item = MenuMaterial.renamed("PLAYER_HEAD", "SKULL_ITEM", (short) 3).createItem();
        ItemMeta rawMeta = item.getItemMeta();
        if (rawMeta instanceof SkullMeta) {
            SkullMeta meta = (SkullMeta) rawMeta;
            meta.setOwner(player.getName());
            meta.setDisplayName(color("&b" + player.getName() + " 的个人设置"));
            meta.setLore(color(Arrays.asList(
                    "&7昵称: &r" + PersonalMenuText.profileValueOrDefault(
                            plugin.getProfileService().getEffectiveNick(player.getUniqueId()), player.getName(),
                            plugin.getNickManager()::colorize),
                    "&7称号: &r" + PersonalMenuText.profileValue(
                            plugin.getProfileService().getEffectivePrefix(player.getUniqueId()),
                            plugin.getNickManager()::colorize),
                    "&7后缀: &r" + PersonalMenuText.profileValue(
                            plugin.getProfileService().getEffectiveSuffix(player.getUniqueId()),
                            plugin.getNickManager()::colorize),
                    "&7聊天: &f" + describeStyle(plugin.getChatColorService().getStyle(player.getUniqueId()))
            )));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack featureItem(Player player, ProfileType type, Material material, String name, String description) {
        boolean enabled = isProfileEnabled(type);
        boolean permission = player.hasPermission(profilePermission(type));
        boolean context = enabled && plugin.getProfileService().isAllowed(player, type);
        String current = effectiveProfileValue(player, type);
        ItemStack icon = enabled && permission && context
                ? new ItemStack(material)
                : MenuMaterial.renamed("IRON_BARS", "IRON_FENCE", (short) 0).createItem();
        return item(icon,
                enabled && permission && context ? name : "&c" + ChatColor.stripColor(color(name)),
                "&7" + description,
                "&7当前: &r" + PersonalMenuText.profileValue(current, plugin.getNickManager()::colorize),
                status(enabled, permission, context));
    }

    private ItemStack chatFeatureItem(Player player) {
        boolean enabled = isChatEnabled();
        boolean permission = hasChatPermission(player, "use", "lobbyaddon.chatcolor.use");
        ItemStack icon = enabled && permission
                ? new ItemStack(Material.REDSTONE)
                : MenuMaterial.renamed("IRON_BARS", "IRON_FENCE", (short) 0).createItem();
        return item(icon,
                enabled && permission ? "&d聊天颜色" : "&c聊天颜色",
                "&7选择单色、渐变与文字格式",
                "&7当前: &f" + describeStyle(plugin.getChatColorService().getStyle(player.getUniqueId())),
                enabled ? (permission ? "&e点击打开" : "&c你没有使用权限") : "&c功能当前未启用");
    }

    private ItemStack profileSummary(Player player, ProfileType type) {
        List<String> lore = new ArrayList<>();
        lore.add("&7当前: &r" + PersonalMenuText.profileValue(
                effectiveProfileValue(player, type), plugin.getNickManager()::colorize));
        if (plugin.getProfileService().hasTemporary(type, player.getUniqueId())) {
            long remaining = plugin.getProfileService().getExpiry(type, player.getUniqueId())
                    - System.currentTimeMillis();
            lore.add("&e临时样式，剩余 " + ProfileService.formatDuration(Math.max(0L, remaining)));
        } else {
            lore.add("&8当前为永久样式或服务器默认");
        }
        long cooldown = plugin.getProfileService().getCooldownRemaining(player.getUniqueId(), type);
        if (cooldown > 0L) {
            lore.add("&c冷却剩余 " + ProfileService.formatDuration(cooldown));
        }
        return item(profileMaterial(type), 0, profileColor(type) + profileLabel(type),
                lore.toArray(new String[0]));
    }

    private void addDuration(Inventory inventory, PersonalMenuHolder holder, Player player, ProfileType type,
                             int slot, long duration, String label) {
        boolean temporaryEnabled = plugin.getConfig().getBoolean("temporary.enabled", true);
        long maximum = Math.max(1L,
                plugin.getConfig().getLong("temporary.max-duration-seconds", 604800L)) * 1000L;
        boolean available = temporaryEnabled && duration <= maximum && canUseProfile(player, type);
        set(inventory, holder, slot, item(MenuMaterial.renamed("CLOCK", "WATCH", (short) 0),
                available ? "&e临时 " + label : "&c临时 " + label,
                "&7到期后自动恢复原设置", clickStatus(available)),
                Action.profileInput(type, duration));
    }

    private void addFormat(Inventory inventory, PersonalMenuHolder holder, Player player, ChatStyle current,
                           int slot, ChatFormat format, Material material, String label) {
        boolean allowed = plugin.getConfig().getBoolean("chat-color.formats.enabled", true)
                && formatAllowed(format)
                && hasChatPermission(player, "format", "lobbyaddon.chatcolor.format")
                && (format != ChatFormat.OBFUSCATED
                || hasChatPermission(player, "obfuscated", "lobbyaddon.chatcolor.obfuscated"));
        boolean active = current.hasFormat(format);
        set(inventory, holder, slot, item(material, 0, active ? "&a" + label + "：开启" : "&7" + label + "：关闭",
                "&7点击切换文字格式", allowed ? (active ? "&e点击关闭" : "&e点击开启") : "&c当前不可用"),
                Action.value(ActionType.CHAT_FORMAT, format.getKey()));
    }

    private void decorate(Inventory inventory, short base, short accent) {
        fill(inventory, base);
        for (int slot : new int[]{0, 1, 7, 8, 9, 17, 18, 19, 25, 26}) {
            inventory.setItem(slot, pane(accent));
        }
    }

    private void frameRows(Inventory inventory, short accent) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            int column = slot % 9;
            if (slot < 9 || slot >= inventory.getSize() - 9 || column == 0 || column == 8) {
                inventory.setItem(slot, pane(accent));
            }
        }
    }

    private void fill(Inventory inventory, short data) {
        ItemStack filler = pane(data);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private static ItemStack pane(short data) {
        return item(MenuMaterial.stainedGlassPane(data), " ");
    }

    private static void set(Inventory inventory, PersonalMenuHolder holder, int slot, ItemStack item, Action action) {
        inventory.setItem(slot, item);
        holder.put(slot, action);
    }

    private static ItemStack item(Material material, int data, String name, String... lore) {
        return item(new ItemStack(material), name, lore);
    }

    private static ItemStack item(MenuMaterial material, String name, String... lore) {
        return item(material.createItem(), name, lore);
    }

    private static ItemStack item(ItemStack item, String name, String... lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            if (lore != null && lore.length > 0) {
                meta.setLore(color(Arrays.asList(lore)));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean modernEnabled(String key) {
        return plugin.getConfig().getBoolean("chat-color.modern.enabled", true)
                && plugin.getConfig().getBoolean("chat-color.modern." + key, true);
    }

    private boolean legacyAllowed(char code) {
        List<String> configured = plugin.getConfig().getStringList("chat-color.legacy.allowed-colors");
        if (configured.isEmpty()) {
            return true;
        }
        for (String entry : configured) {
            Character parsed = ChatColorParser.parseLegacy(entry);
            if (parsed != null && parsed == code) {
                return true;
            }
        }
        return false;
    }

    private boolean formatAllowed(ChatFormat format) {
        List<String> configured = plugin.getConfig().getStringList("chat-color.formats.allowed");
        if (configured.isEmpty()) {
            return true;
        }
        for (String entry : configured) {
            if (ChatFormat.fromInput(entry) == format) {
                return true;
            }
        }
        return false;
    }

    private String effectiveProfileValue(Player player, ProfileType type) {
        if (type == ProfileType.NICK) {
            return plugin.getProfileService().getEffectiveNick(player.getUniqueId());
        }
        if (type == ProfileType.PREFIX) {
            return plugin.getProfileService().getEffectivePrefix(player.getUniqueId());
        }
        return plugin.getProfileService().getEffectiveSuffix(player.getUniqueId());
    }

    private String configTitle(String path, String fallback) {
        return color(plugin.getConfig().getString(path, fallback));
    }

    private static String trimTitle(String title) {
        String value = title == null ? "" : title;
        return value.length() <= 32 ? value : value.substring(0, 32);
    }

    private static String status(boolean enabled, boolean permission, boolean context) {
        if (!enabled) {
            return "&c功能当前未启用";
        }
        if (!permission) {
            return "&c你没有使用权限";
        }
        return context ? "&e点击打开" : "&c当前世界或子服不可用";
    }

    private static String clickStatus(boolean allowed) {
        return allowed ? "&e点击选择" : "&c当前不可用";
    }

    private static String profileLabel(ProfileType type) {
        if (type == ProfileType.NICK) {
            return "昵称";
        }
        return type == ProfileType.PREFIX ? "自定义称号" : "自定义后缀";
    }

    private static String profileCommand(ProfileType type) {
        if (type == ProfileType.NICK) {
            return "nick";
        }
        return type == ProfileType.PREFIX ? "custom" : "suffix";
    }

    private static Material profileMaterial(ProfileType type) {
        if (type == ProfileType.NICK) {
            return Material.NAME_TAG;
        }
        return type == ProfileType.PREFIX ? Material.GOLD_INGOT : Material.IRON_INGOT;
    }

    private static String profileColor(ProfileType type) {
        if (type == ProfileType.NICK) {
            return "&b";
        }
        return type == ProfileType.PREFIX ? "&6" : "&f";
    }

    private static short profileAccent(ProfileType type) {
        if (type == ProfileType.NICK) {
            return 3;
        }
        return type == ProfileType.PREFIX ? (short) 1 : (short) 0;
    }

    private static String describeStyle(ChatStyle style) {
        if (style == null || style.isEmpty()) {
            return "默认";
        }
        StringBuilder description = new StringBuilder();
        if (style.getMode() == ChatStyle.Mode.LEGACY) {
            description.append('&').append(style.getLegacyColor()).append("传统色");
        } else if (style.getMode() == ChatStyle.Mode.HEX) {
            description.append("Hex ").append(ChatStyleCodec.primary(style));
        } else if (style.getMode() == ChatStyle.Mode.GRADIENT) {
            description.append("渐变 ").append(ChatStyleCodec.primary(style)).append(" -> ")
                    .append(ChatStyleCodec.secondary(style));
        }
        List<String> formats = ChatStyleCodec.formats(style);
        if (!formats.isEmpty()) {
            if (description.length() > 0) {
                description.append(" + ");
            }
            description.append(String.join(", ", formats));
        }
        return description.length() == 0 ? "默认" : description.toString();
    }

    private static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }

    private static List<String> color(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String value : values) {
            result.add(color(value));
        }
        return result;
    }

    public enum InputKind {
        PROFILE,
        HEX,
        GRADIENT
    }

    public static final class PendingInput {
        private final InputKind kind;
        private final ProfileType profileType;
        private final long durationMillis;
        private long expiresAt;

        private PendingInput(InputKind kind, ProfileType profileType, long durationMillis) {
            this.kind = kind;
            this.profileType = profileType;
            this.durationMillis = durationMillis;
        }

        public InputKind getKind() {
            return kind;
        }

        public ProfileType getProfileType() {
            return profileType;
        }

        public long getDurationMillis() {
            return durationMillis;
        }
    }
}
