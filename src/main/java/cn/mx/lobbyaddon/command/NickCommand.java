package cn.mx.lobbyaddon.command;

import cn.mx.lobbyaddon.LobbyAddon;
import cn.mx.lobbyaddon.config.LobbyConfig;
import cn.mx.lobbyaddon.profile.ProfileOperationResult;
import cn.mx.lobbyaddon.profile.ProfileService;
import cn.mx.lobbyaddon.profile.ProfileType;
import cn.mx.lobbyaddon.service.NickManager;
import cn.mx.lobbyaddon.util.BukkitCompat;
import cn.mx.lobbyaddon.util.ProfileTextRules;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class NickCommand implements CommandExecutor, TabCompleter {

    private final LobbyAddon plugin;
    private final NickManager nickManager;
    private final LobbyConfig config;

    public NickCommand(LobbyAddon plugin, NickManager nickManager, LobbyConfig config) {
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
        if (!player.hasPermission(config.nickUse)) {
            CommandMessages.noPermission(player);
            return true;
        }
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        Player target = args.length >= 2 ? Bukkit.getPlayer(args[0]) : null;
        boolean menuSelf = plugin.getPersonalMenuService() != null
                && plugin.getPersonalMenuService().isForcingSelfNick(player.getUniqueId());
        if (targetsOther(menuSelf, player.hasPermission(config.nickOthers), args.length, target != null)) {
            return handleTargetCommand(player, target, args);
        }
        return handleSelfCommand(player, args);
    }

    static boolean targetsOther(boolean menuSelf, boolean hasOthersPermission, int argumentCount,
                                boolean targetOnline) {
        return !menuSelf && hasOthersPermission && argumentCount >= 2 && targetOnline;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.add("reset");
            suggestions.add("temp");
            if (sender.hasPermission(config.nickOthers)) {
                for (Player player : BukkitCompat.getOnlinePlayers()) {
                    suggestions.add(player.getName());
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("temp")) {
            suggestions.add("30m");
            suggestions.add("1h");
            suggestions.add("1d");
        } else if (args.length == 2 && sender.hasPermission(config.nickOthers)
                && Bukkit.getPlayer(args[0]) != null) {
            suggestions.add("reset");
            suggestions.add("temp");
        } else if (args.length == 3 && sender.hasPermission(config.nickOthers)
                && Bukkit.getPlayer(args[0]) != null && args[1].equalsIgnoreCase("temp")) {
            suggestions.add("30m");
            suggestions.add("1h");
            suggestions.add("1d");
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
        List<String> result = new ArrayList<>();
        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase().startsWith(prefix)) {
                result.add(suggestion);
            }
        }
        return result;
    }

    private boolean handleSelfCommand(Player player, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reset")) {
            ProfileOperationResult result = plugin.getProfileService().reset(player, ProfileType.NICK);
            if (CommandMessages.reportProfileResult(player, result)) {
                CommandMessages.success(player, "你的昵称已重置为: \u00a7f" + player.getName());
            }
            return true;
        }
        ParsedValue parsed = parseValue(player, args, 0, "/nick temp <时长> <昵称>");
        if (parsed == null || !validate(player, player, parsed.value)) {
            return true;
        }
        ProfileOperationResult result = plugin.getProfileService()
                .requestOrApply(player, ProfileType.NICK, parsed.value, parsed.durationMillis);
        if (CommandMessages.reportProfileResult(player, result)) {
            String duration = parsed.durationMillis > 0L
                    ? "，持续 " + ProfileService.formatDuration(parsed.durationMillis) : "";
            CommandMessages.success(player, "你的昵称已设置为: \u00a7r" + nickManager.colorize(parsed.value) + duration);
        }
        return true;
    }

    private boolean handleTargetCommand(Player actor, Player target, String[] args) {
        if (args.length == 2 && args[1].equalsIgnoreCase("reset")) {
            ProfileOperationResult result = plugin.getProfileService().resetAdministrative(actor, target, ProfileType.NICK);
            if (CommandMessages.reportProfileResult(actor, result)) {
                CommandMessages.success(actor, "已重置 " + target.getName() + " 的昵称。");
                CommandMessages.info(target, "你的昵称已由 " + actor.getName() + " 重置。");
            }
            return true;
        }
        ParsedValue parsed = parseValue(actor, args, 1, "/nick <玩家> temp <时长> <昵称>");
        if (parsed == null || !validate(actor, target, parsed.value)) {
            return true;
        }
        ProfileOperationResult result = plugin.getProfileService()
                .applyAdministrative(actor, target, ProfileType.NICK, parsed.value, parsed.durationMillis);
        if (CommandMessages.reportProfileResult(actor, result)) {
            String duration = parsed.durationMillis > 0L
                    ? "，持续 " + ProfileService.formatDuration(parsed.durationMillis) : "";
            CommandMessages.success(actor, "已将 " + target.getName() + " 的昵称设置为: \u00a7r"
                    + nickManager.colorize(parsed.value) + duration);
            CommandMessages.info(target, "你的昵称已由 " + actor.getName() + " 设置为: \u00a7r"
                    + nickManager.colorize(parsed.value) + duration);
        }
        return true;
    }

    private ParsedValue parseValue(Player player, String[] args, int start, String usage) {
        if (start >= args.length) {
            CommandMessages.usage(player, usage);
            return null;
        }
        if (!args[start].equalsIgnoreCase("temp")) {
            return new ParsedValue(String.join(" ", Arrays.copyOfRange(args, start, args.length)), 0L);
        }
        if (start + 2 >= args.length) {
            CommandMessages.usage(player, usage);
            return null;
        }
        long duration = ProfileService.parseDurationMillis(args[start + 1]);
        if (duration <= 0L) {
            CommandMessages.error(player, "时长格式无效，请使用 30m、1h 或 1d。");
            return null;
        }
        return new ParsedValue(String.join(" ", Arrays.copyOfRange(args, start + 2, args.length)), duration);
    }

    private boolean validate(Player operator, Player target, String rawNick) {
        String validationError = plugin.getProfileService().validate(operator, ProfileType.NICK, rawNick,
                target.getUniqueId());
        if (validationError != null) {
            CommandMessages.error(operator, validationError);
            return false;
        }
        if (ProfileTextRules.usesColorCode(rawNick, config.colorCode) && !operator.hasPermission(config.nickColor)) {
            CommandMessages.error(operator, "你没有使用颜色代码的权限。");
            return false;
        }
        String stripped = nickManager.stripColor(nickManager.colorize(rawNick));
        if (!operator.hasPermission(config.nickBypass)) {
            if (stripped.length() < config.nickMinLength) {
                CommandMessages.error(operator, "昵称长度不能少于 " + config.nickMinLength + " 个字符。");
                return false;
            }
            if (stripped.length() > config.nickMaxLength) {
                CommandMessages.error(operator, "昵称长度不能超过 " + config.nickMaxLength + " 个字符。");
                return false;
            }
        }
        UUID existing = plugin.getProfileService().findUuidByNickname(stripped);
        if (existing != null && !existing.equals(target.getUniqueId())) {
            Player owner = Bukkit.getPlayer(existing);
            CommandMessages.error(operator, "该昵称已被 " + (owner == null ? "其他玩家" : owner.getName()) + " 使用。");
            return false;
        }
        for (Player online : BukkitCompat.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(stripped) && !online.getUniqueId().equals(target.getUniqueId())) {
                CommandMessages.error(operator, "不能使用其他玩家的真实名称作为昵称。");
                return false;
            }
        }
        return true;
    }

    private void sendHelp(Player player) {
        String nick = plugin.getProfileService().getEffectiveNick(player.getUniqueId());
        CommandMessages.helpHeader(player, "昵称管理");
        CommandMessages.helpLine(player, "/nick <昵称>", "设置你的昵称");
        CommandMessages.helpLine(player, "/nick temp <时长> <昵称>", "设置临时昵称，例如 1h");
        CommandMessages.helpLine(player, "/nick reset", "重置你的昵称");
        if (nick != null) {
            CommandMessages.helpInfo(player, "当前昵称", nickManager.colorize(nick));
        }
        if (player.hasPermission(config.nickOthers)) {
            CommandMessages.helpLine(player, "/nick <玩家> <昵称>", "设置其他在线玩家的昵称");
            CommandMessages.helpLine(player, "/nick <玩家> temp <时长> <昵称>", "设置其他玩家的临时昵称");
            CommandMessages.helpLine(player, "/nick <玩家> reset", "重置其他玩家的昵称");
        }
        CommandMessages.helpFooter(player);
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
