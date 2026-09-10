package cn.mx.lobbyaddon.command;

import cn.mx.lobbyaddon.LobbyAddon;
import cn.mx.lobbyaddon.config.LobbyConfig;
import cn.mx.lobbyaddon.profile.ProfileOperationResult;
import cn.mx.lobbyaddon.profile.ProfileService;
import cn.mx.lobbyaddon.profile.ProfileType;
import cn.mx.lobbyaddon.service.NickManager;
import cn.mx.lobbyaddon.util.ProfileTextRules;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SuffixCommand implements CommandExecutor, TabCompleter {

    private final LobbyAddon plugin;
    private final NickManager nickManager;
    private final LobbyConfig config;

    public SuffixCommand(LobbyAddon plugin, NickManager nickManager, LobbyConfig config) {
        this.plugin = plugin;
        this.nickManager = nickManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            CommandMessages.playersOnly(sender);
            return true;
        }
        Player player = (Player) sender;
        if (!config.suffixEnabled) {
            CommandMessages.error(player, "自定义后缀功能当前未启用。");
            return true;
        }
        if (!player.hasPermission(config.suffixUse)) {
            CommandMessages.noPermission(player);
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reset")) {
            ProfileOperationResult result = plugin.getProfileService().reset(player, ProfileType.SUFFIX);
            if (CommandMessages.reportProfileResult(player, result)) {
                CommandMessages.success(player, "你的自定义后缀已重置。");
            }
            return true;
        }

        ParsedValue parsed = parseValue(player, args);
        if (parsed == null || !validate(player, parsed.value)) {
            return true;
        }
        ProfileOperationResult result = plugin.getProfileService()
                .requestOrApply(player, ProfileType.SUFFIX, parsed.value, parsed.durationMillis);
        if (CommandMessages.reportProfileResult(player, result)) {
            String duration = parsed.durationMillis > 0L
                    ? "，持续 " + ProfileService.formatDuration(parsed.durationMillis) : "";
            CommandMessages.success(player, "你的自定义后缀已设置为: \u00a7r"
                    + plugin.getProfileService().formatSuffix(parsed.value) + duration);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.add("reset");
            suggestions.add("temp");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("temp")) {
            suggestions.add("30m");
            suggestions.add("1h");
            suggestions.add("1d");
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
        return suggestions.stream().filter(value -> value.startsWith(prefix)).collect(Collectors.toList());
    }

    private void sendHelp(Player player) {
        CommandMessages.helpHeader(player, "自定义后缀");
        CommandMessages.helpLine(player, "/suffix <后缀>", "设置带灰色括号的自定义后缀");
        CommandMessages.helpLine(player, "/suffix temp <时长> <后缀>", "设置临时后缀，例如 1h");
        CommandMessages.helpLine(player, "/suffix reset", "重置自定义后缀");
        CommandMessages.helpFooter(player);
    }

    private ParsedValue parseValue(Player player, String[] args) {
        if (!args[0].equalsIgnoreCase("temp")) {
            return new ParsedValue(String.join(" ", args), 0L);
        }
        if (args.length < 3) {
            CommandMessages.usage(player, "/suffix temp <时长> <后缀>");
            return null;
        }
        long duration = ProfileService.parseDurationMillis(args[1]);
        if (duration <= 0L) {
            CommandMessages.error(player, "时长格式无效，请使用 30m、1h 或 1d。");
            return null;
        }
        return new ParsedValue(String.join(" ", Arrays.copyOfRange(args, 2, args.length)), duration);
    }

    private boolean validate(Player player, String rawSuffix) {
        String validationError = plugin.getProfileService().validate(player, ProfileType.SUFFIX, rawSuffix,
                player.getUniqueId());
        if (validationError != null) {
            CommandMessages.error(player, validationError);
            return false;
        }
        if (ProfileTextRules.usesColorCode(rawSuffix, config.colorCode) && !player.hasPermission(config.suffixColor)) {
            CommandMessages.error(player, "你没有使用颜色代码的权限。");
            return false;
        }
        String stripped = nickManager.stripColor(nickManager.colorize(rawSuffix));
        if (!player.hasPermission(config.suffixBypass)) {
            if (stripped.length() < config.suffixMinLength) {
                CommandMessages.error(player, "后缀长度不能少于 " + config.suffixMinLength + " 个字符。");
                return false;
            }
            if (stripped.length() > config.suffixMaxLength) {
                CommandMessages.error(player, "后缀长度不能超过 " + config.suffixMaxLength + " 个字符。");
                return false;
            }
        }
        for (String banned : config.suffixBannedWords) {
            if (stripped.toLowerCase().contains(banned.toLowerCase())) {
                CommandMessages.error(player, "后缀中包含禁用词: " + banned);
                return false;
            }
        }
        return true;
    }

    private static final class ParsedValue {
        private final String value;
        private final long durationMillis;

        private ParsedValue(String value, long durationMillis) {
            this.value = value;
            this.durationMillis = durationMillis;
        }
    }
}
