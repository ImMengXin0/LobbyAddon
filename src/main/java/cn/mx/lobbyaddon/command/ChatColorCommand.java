package cn.mx.lobbyaddon.command;

import cn.mx.lobbyaddon.LobbyAddon;
import cn.mx.lobbyaddon.chat.ChatColorParser;
import cn.mx.lobbyaddon.chat.ChatColorService;
import cn.mx.lobbyaddon.chat.ChatFormat;
import cn.mx.lobbyaddon.chat.ChatStyle;
import cn.mx.lobbyaddon.chat.ChatStyleCodec;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ChatColorCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "legacy", "hex", "gradient", "preset", "format", "preview", "reset");

    private final LobbyAddon plugin;

    public ChatColorCommand(LobbyAddon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            CommandMessages.playersOnly(sender);
            return true;
        }
        Player player = (Player) sender;
        if (!plugin.getConfig().getBoolean("chat-color.enabled", true)) {
            CommandMessages.error(player, "聊天颜色系统当前未启用。");
            return true;
        }
        if (!hasConfiguredPermission(player, "use", "lobbyaddon.chatcolor.use")) {
            CommandMessages.noPermission(player);
            return true;
        }
        if (args.length == 0) {
            sendHelp(player, label);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if ("legacy".equals(subcommand) || "color".equals(subcommand)) {
            return setLegacy(player, args);
        }
        if ("hex".equals(subcommand) || "solid".equals(subcommand)) {
            return setHex(player, args);
        }
        if ("gradient".equals(subcommand) || "grad".equals(subcommand)) {
            return setGradient(player, args);
        }
        if ("preset".equals(subcommand)) {
            return setPreset(player, args);
        }
        if ("format".equals(subcommand) || "style".equals(subcommand)) {
            return setFormat(player, args);
        }
        if ("preview".equals(subcommand)) {
            return preview(player, args);
        }
        if ("reset".equals(subcommand) || "clear".equals(subcommand)) {
            plugin.getChatColorService().reset(player.getUniqueId());
            CommandMessages.success(player, "聊天颜色与格式已重置。");
            return true;
        }
        CommandMessages.error(player, "未知的聊天颜色子命令。");
        sendHelp(player, label);
        return true;
    }

    private boolean setLegacy(Player player, String[] args) {
        if (!plugin.getConfig().getBoolean("chat-color.legacy.enabled", true)) {
            CommandMessages.error(player, "传统聊天颜色模块当前未启用。");
            return true;
        }
        if (!hasConfiguredPermission(player, "legacy", "lobbyaddon.chatcolor.legacy")) {
            CommandMessages.noPermission(player);
            return true;
        }
        if (args.length != 2) {
            CommandMessages.usage(player, "/chatcolor legacy <&0-&f|颜色名>");
            return true;
        }
        Character color = ChatColorParser.parseLegacy(args[1]);
        if (color == null || !isLegacyAllowed(color)) {
            CommandMessages.error(player, "无效或未开放的传统颜色。可用颜色: " + legacyChoices());
            return true;
        }
        ChatStyle style = current(player).withLegacyColor(color);
        saveAndPreview(player, style, "传统聊天颜色已设置为 &" + color + "。");
        return true;
    }

    private boolean setHex(Player player, String[] args) {
        if (!modernFeatureAvailable("hex-enabled")) {
            CommandMessages.error(player, "Hex 聊天颜色模块当前未启用。");
            return true;
        }
        if (!hasConfiguredPermission(player, "hex", "lobbyaddon.chatcolor.hex")) {
            CommandMessages.noPermission(player);
            return true;
        }
        if (args.length != 2) {
            CommandMessages.usage(player, "/chatcolor hex <#RRGGBB>");
            return true;
        }
        Integer color = ChatColorParser.parseHex(args[1]);
        if (color == null) {
            CommandMessages.error(player, "Hex 颜色必须是六位十六进制，例如 #12ABEF。");
            return true;
        }
        ChatStyle style = current(player).withHexColor(color);
        saveAndPreview(player, style, "Hex 聊天颜色已设置为 " + ChatStyleCodec.primary(style) + "。");
        return true;
    }

    private boolean setGradient(Player player, String[] args) {
        if (!modernFeatureAvailable("gradient-enabled")) {
            CommandMessages.error(player, "渐变聊天颜色模块当前未启用。");
            return true;
        }
        if (!hasConfiguredPermission(player, "gradient", "lobbyaddon.chatcolor.gradient")) {
            CommandMessages.noPermission(player);
            return true;
        }
        if (args.length != 3) {
            CommandMessages.usage(player, "/chatcolor gradient <#起始色> <#结束色>");
            return true;
        }
        Integer start = ChatColorParser.parseHex(args[1]);
        Integer end = ChatColorParser.parseHex(args[2]);
        if (start == null || end == null) {
            CommandMessages.error(player, "渐变起始色和结束色都必须是 #RRGGBB。");
            return true;
        }
        ChatStyle style = current(player).withGradient(start, end);
        saveAndPreview(player, style, "渐变聊天颜色已设置为 "
                + ChatStyleCodec.primary(style) + " -> " + ChatStyleCodec.secondary(style) + "。");
        return true;
    }

    private boolean setPreset(Player player, String[] args) {
        if (!hasConfiguredPermission(player, "preset", "lobbyaddon.chatcolor.preset")) {
            CommandMessages.noPermission(player);
            return true;
        }
        if (args.length != 2) {
            CommandMessages.usage(player, "/chatcolor preset <名称>");
            CommandMessages.info(player, "可用预设: " + presetChoices(player));
            return true;
        }
        ChatColorService service = plugin.getChatColorService();
        ChatStyle preset = service.getPreset(args[1]);
        if (preset == null || !service.isPresetAllowed(player, args[1])) {
            CommandMessages.error(player, "未找到或无权使用该聊天颜色预设。");
            return true;
        }
        saveAndPreview(player, preset, "已应用聊天颜色预设 " + args[1] + "。");
        return true;
    }

    private boolean setFormat(Player player, String[] args) {
        if (!plugin.getConfig().getBoolean("chat-color.formats.enabled", true)) {
            CommandMessages.error(player, "聊天文字格式模块当前未启用。");
            return true;
        }
        if (!hasConfiguredPermission(player, "format", "lobbyaddon.chatcolor.format")) {
            CommandMessages.noPermission(player);
            return true;
        }
        if (args.length == 2 && "reset".equalsIgnoreCase(args[1])) {
            ChatStyle style = current(player).withoutFormats();
            saveAndPreview(player, style, "聊天文字格式已清除。");
            return true;
        }
        if (args.length < 2 || args.length > 3) {
            CommandMessages.usage(player, "/chatcolor format <bold|italic|underline|strikethrough|obfuscated> [on|off|toggle]");
            return true;
        }
        ChatFormat format = ChatFormat.fromInput(args[1]);
        if (format == null || !isFormatAllowed(format)) {
            CommandMessages.error(player, "无效或未开放的文字格式。可用格式: " + formatChoices());
            return true;
        }
        if (format == ChatFormat.OBFUSCATED
                && !hasConfiguredPermission(player, "obfuscated", "lobbyaddon.chatcolor.obfuscated")) {
            CommandMessages.noPermission(player);
            return true;
        }
        Boolean requested = args.length == 3 ? parseState(args[2]) : null;
        if (args.length == 3 && requested == null && !"toggle".equalsIgnoreCase(args[2])) {
            CommandMessages.error(player, "格式状态仅支持 on、off 或 toggle。");
            return true;
        }
        ChatStyle current = current(player);
        boolean enabled = requested == null ? !current.hasFormat(format) : requested;
        ChatStyle style = current.withFormat(format, enabled);
        saveAndPreview(player, style, format.getKey() + " 已" + (enabled ? "启用" : "关闭") + "。");
        return true;
    }

    private boolean preview(Player player, String[] args) {
        String text;
        if (args.length > 1) {
            text = join(args, 1);
        } else {
            text = plugin.getConfig().getString("chat-color.preview-text", "这是一条聊天颜色预览消息");
        }
        CommandMessages.info(player, "预览: " + plugin.getChatColorService().render(player, text));
        return true;
    }

    private void sendHelp(Player player, String label) {
        ChatStyle style = current(player);
        CommandMessages.helpHeader(player, "聊天颜色");
        CommandMessages.helpInfo(player, "当前", describe(style));
        if (plugin.getConfig().getBoolean("chat-color.legacy.enabled", true)
                && hasConfiguredPermission(player, "legacy", "lobbyaddon.chatcolor.legacy")) {
            CommandMessages.helpLine(player, "/" + label + " legacy <颜色>", "传统 &0-&f 聊天颜色");
        }
        if (modernFeatureAvailable("hex-enabled")
                && hasConfiguredPermission(player, "hex", "lobbyaddon.chatcolor.hex")) {
            CommandMessages.helpLine(player, "/" + label + " hex <#RRGGBB>", "设置十六进制单色");
        }
        if (modernFeatureAvailable("gradient-enabled")
                && hasConfiguredPermission(player, "gradient", "lobbyaddon.chatcolor.gradient")) {
            CommandMessages.helpLine(player, "/" + label + " gradient <#起始> <#结束>", "设置渐变颜色");
        }
        if (plugin.getConfig().getBoolean("chat-color.formats.enabled", true)
                && hasConfiguredPermission(player, "format", "lobbyaddon.chatcolor.format")) {
            CommandMessages.helpLine(player, "/" + label + " format <格式> [状态]", "切换加粗等文字格式");
        }
        if (hasConfiguredPermission(player, "preset", "lobbyaddon.chatcolor.preset")) {
            CommandMessages.helpLine(player, "/" + label + " preset <名称>", "应用组合预设: " + presetChoices(player));
        }
        CommandMessages.helpLine(player, "/" + label + " preview [文字]", "预览当前聊天样式");
        CommandMessages.helpLine(player, "/" + label + " reset", "重置颜色与格式");
        CommandMessages.helpFooter(player);
    }

    private void saveAndPreview(Player player, ChatStyle style, String message) {
        plugin.getChatColorService().setStyle(player.getUniqueId(), style);
        CommandMessages.success(player, message);
        CommandMessages.info(player, "预览: " + plugin.getChatColorService().render(player,
                plugin.getConfig().getString("chat-color.preview-text", "这是一条聊天颜色预览消息")));
    }

    private ChatStyle current(Player player) {
        return plugin.getChatColorService().getStyle(player.getUniqueId());
    }

    private boolean modernFeatureAvailable(String key) {
        return plugin.getConfig().getBoolean("chat-color.modern.enabled", true)
                && plugin.getConfig().getBoolean("chat-color.modern." + key, true);
    }

    private boolean hasConfiguredPermission(Player player, String key, String defaultPermission) {
        String permission = plugin.getConfig().getString("chat-color.permissions." + key, defaultPermission);
        return permission == null || permission.trim().isEmpty() || player.hasPermission(permission);
    }

    private boolean isLegacyAllowed(char color) {
        List<String> allowed = plugin.getConfig().getStringList("chat-color.legacy.allowed-colors");
        if (allowed.isEmpty()) {
            return true;
        }
        for (String entry : allowed) {
            Character parsed = ChatColorParser.parseLegacy(entry);
            if (parsed != null && parsed == color) {
                return true;
            }
        }
        return false;
    }

    private boolean isFormatAllowed(ChatFormat format) {
        List<String> allowed = plugin.getConfig().getStringList("chat-color.formats.allowed");
        if (allowed.isEmpty()) {
            return true;
        }
        for (String entry : allowed) {
            if (format == ChatFormat.fromInput(entry)) {
                return true;
            }
        }
        return false;
    }

    private String legacyChoices() {
        List<String> allowed = plugin.getConfig().getStringList("chat-color.legacy.allowed-colors");
        return allowed.isEmpty() ? "&0-&f" : String.join(", ", allowed);
    }

    private String formatChoices() {
        List<String> allowed = plugin.getConfig().getStringList("chat-color.formats.allowed");
        return allowed.isEmpty() ? "bold, italic, underline, strikethrough, obfuscated"
                : String.join(", ", allowed);
    }

    private String presetChoices(Player player) {
        List<String> allowed = new ArrayList<>();
        for (String preset : plugin.getChatColorService().getPresetNames()) {
            if (plugin.getChatColorService().isPresetAllowed(player, preset)) {
                allowed.add(preset);
            }
        }
        return allowed.isEmpty() ? "无" : String.join(", ", allowed);
    }

    private static Boolean parseState(String input) {
        if ("on".equalsIgnoreCase(input) || "true".equalsIgnoreCase(input)
                || "enable".equalsIgnoreCase(input)) {
            return Boolean.TRUE;
        }
        if ("off".equalsIgnoreCase(input) || "false".equalsIgnoreCase(input)
                || "disable".equalsIgnoreCase(input)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static String describe(ChatStyle style) {
        StringBuilder result = new StringBuilder(ChatStyleCodec.mode(style));
        String primary = ChatStyleCodec.primary(style);
        String secondary = ChatStyleCodec.secondary(style);
        if (primary != null) {
            result.append(' ').append(primary);
        }
        if (secondary != null) {
            result.append(" -> ").append(secondary);
        }
        List<String> formats = ChatStyleCodec.formats(style);
        if (!formats.isEmpty()) {
            result.append(" + ").append(String.join(", ", formats));
        }
        return result.toString();
    }

    private static String join(String[] values, int start) {
        StringBuilder result = new StringBuilder();
        for (int index = start; index < values.length; index++) {
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(values[index]);
        }
        return result.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }
        Player player = (Player) sender;
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.addAll(SUBCOMMANDS);
        } else if (args.length == 2 && ("legacy".equalsIgnoreCase(args[0]) || "color".equalsIgnoreCase(args[0]))) {
            suggestions.addAll(plugin.getConfig().getStringList("chat-color.legacy.allowed-colors"));
        } else if (args.length == 2 && "preset".equalsIgnoreCase(args[0])) {
            for (String preset : plugin.getChatColorService().getPresetNames()) {
                if (plugin.getChatColorService().isPresetAllowed(player, preset)) {
                    suggestions.add(preset);
                }
            }
        } else if (args.length == 2 && ("format".equalsIgnoreCase(args[0]) || "style".equalsIgnoreCase(args[0]))) {
            suggestions.addAll(plugin.getConfig().getStringList("chat-color.formats.allowed"));
            suggestions.add("reset");
        } else if (args.length == 3 && ("format".equalsIgnoreCase(args[0]) || "style".equalsIgnoreCase(args[0]))) {
            suggestions.addAll(Arrays.asList("on", "off", "toggle"));
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                result.add(suggestion);
            }
        }
        return result;
    }
}
