package cn.mx.lobbyaddon.command;

import cn.mx.lobbyaddon.LobbyAddon;
import cn.mx.lobbyaddon.profile.AuditEntry;
import cn.mx.lobbyaddon.profile.ProfileOperationResult;
import cn.mx.lobbyaddon.profile.ProfileRequest;
import cn.mx.lobbyaddon.util.BukkitCompat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class LobbyCommand implements CommandExecutor, TabCompleter {

    private final LobbyAddon plugin;

    public LobbyCommand(LobbyAddon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if ("reload".equals(subcommand)) {
            return reload(sender, args);
        }
        if ("info".equals(subcommand)) {
            return info(sender, args);
        }
        if ("debug".equals(subcommand)) {
            return debug(sender, args);
        }
        if ("pending".equals(subcommand)) {
            return pending(sender, args);
        }
        if ("approve".equals(subcommand)) {
            return approve(sender, args);
        }
        if ("reject".equals(subcommand)) {
            return reject(sender, args);
        }
        if ("history".equals(subcommand)) {
            return history(sender, args);
        }
        if ("menu".equals(subcommand)) {
            return menu(sender, args);
        }
        if ("papi".equals(subcommand)) {
            return papi(sender, args);
        }
        CommandMessages.error(sender, "未知的子命令。");
        sendHelp(sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            addAllowed(sender, suggestions, "reload", "lobbyaddon.reload");
            addAllowed(sender, suggestions, "info", "lobbyaddon.info");
            addAllowed(sender, suggestions, "debug", "lobbyaddon.debug");
            addAllowed(sender, suggestions, "pending", "lobbyaddon.approval.review");
            addAllowed(sender, suggestions, "approve", "lobbyaddon.approval.review");
            addAllowed(sender, suggestions, "reject", "lobbyaddon.approval.review");
            addAllowed(sender, suggestions, "history", "lobbyaddon.history");
            addAllowed(sender, suggestions, "menu", "lobbyaddon.cosmetics.use");
            addAllowed(sender, suggestions, "papi", "lobbyaddon.papi");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("approve") || args[0].equalsIgnoreCase("reject"))) {
            if (sender.hasPermission("lobbyaddon.approval.review")) {
                for (ProfileRequest request : plugin.getProfileService().getPendingRequests()) {
                    suggestions.add(request.getId().substring(0, 8));
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("history")) {
            if (sender.hasPermission("lobbyaddon.history")) {
                for (Player player : BukkitCompat.getOnlinePlayers()) {
                    suggestions.add(player.getName());
                }
            }
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

    private boolean reload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lobbyaddon.reload")) {
            CommandMessages.noPermission(sender);
            return true;
        }
        if (args.length != 1) {
            CommandMessages.usage(sender, "/lobbyaddon reload");
            return true;
        }
        plugin.load();
        CommandMessages.success(sender, "插件配置与运行服务已重新加载。");
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lobbyaddon.info")) {
            CommandMessages.noPermission(sender);
            return true;
        }
        if (args.length != 1) {
            CommandMessages.usage(sender, "/lobbyaddon info");
            return true;
        }
        CommandMessages.helpHeader(sender, "运行状态");
        CommandMessages.helpInfo(sender, "版本", plugin.getDescription().getVersion());
        CommandMessages.helpInfo(sender, "在线人数", String.valueOf(BukkitCompat.getOnlinePlayerCount()));
        CommandMessages.helpInfo(sender, "存储", plugin.getStorage() == null ? "未初始化"
                : plugin.getStorage().getClass().getSimpleName() + " (" + plugin.getStorage().getStatus() + ")");
        CommandMessages.helpInfo(sender, "LuckPerms", plugin.getProfileService().isLuckPermsAvailable() ? "已连接" : "未安装");
        CommandMessages.helpInfo(sender, "聊天颜色", plugin.getChatColorService() == null ? "未初始化"
                : plugin.getChatColorService().size() + " 位玩家 (" + (plugin.getChatColorService().isHealthy() ? "正常" : "写入异常") + ")");
        CommandMessages.helpInfo(sender, "Bungee", plugin.getBungeeCounter() == null ? "已禁用"
                : plugin.getBungeeCounter().isFresh() ? "在线" : "缓存过期");
        CommandMessages.helpFooter(sender);
        return true;
    }

    private boolean debug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lobbyaddon.debug")) {
            CommandMessages.noPermission(sender);
            return true;
        }
        if (args.length != 1) {
            CommandMessages.usage(sender, "/lobbyaddon debug");
            return true;
        }
        CommandMessages.info(sender, plugin.getProfileService().getDebugSummary());
        CommandMessages.info(sender, "bungee-last-update=" + (plugin.getBungeeCounter() == null
                ? 0L : plugin.getBungeeCounter().getLastUpdatedAt()));
        CommandMessages.info(sender, "storage=" + (plugin.getStorage() == null
                ? "uninitialized" : plugin.getStorage().getStatus()));
        CommandMessages.info(sender, "chat-colors=" + (plugin.getChatColorService() == null
                ? "uninitialized" : plugin.getChatColorService().size() + ", healthy="
                + plugin.getChatColorService().isHealthy()));
        return true;
    }

    private boolean papi(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lobbyaddon.papi")) {
            CommandMessages.noPermission(sender);
            return true;
        }
        if (args.length != 1) {
            CommandMessages.usage(sender, "/lobbyaddon papi");
            return true;
        }
        CommandMessages.helpHeader(sender, "PlaceholderAPI");
        CommandMessages.helpInfo(sender, "\u4e3b\u524d\u7f00", "%lobbyaddon_<\u53d8\u91cf>%");
        CommandMessages.helpInfo(sender, "\u517c\u5bb9\u524d\u7f00", "%nick_<\u53d8\u91cf>%");
        CommandMessages.helpInfo(sender, "\u4eba\u6570", "%lobbyaddon_online% | %lobbyaddon_max_online%");
        CommandMessages.helpInfo(sender, "Bungee", "%lobbyaddon_online_<server>% | %lobbyaddon_bungee_status%");
        CommandMessages.helpInfo(sender, "\u540d\u79f0", "%lobbyaddon_name% | %lobbyaddon_nick% | %lobbyaddon_realname% | %lobbyaddon_hasnick%");
        CommandMessages.helpInfo(sender, "\u663e\u793a", "%lobbyaddon_display% | %lobbyaddon_prefix% | %lobbyaddon_suffix%");
        CommandMessages.helpInfo(sender, "\u6837\u5f0f\u72b6\u6001", "%lobbyaddon_hasprefix% | %lobbyaddon_hassuffix%");
        CommandMessages.helpInfo(sender, "\u4e34\u65f6\u72b6\u6001", "%lobbyaddon_nick_temporary% | %lobbyaddon_prefix_temporary% | %lobbyaddon_suffix_temporary%");
        CommandMessages.helpInfo(sender, "\u5230\u671f\u65f6\u95f4", "%lobbyaddon_nick_expires_at% | %lobbyaddon_prefix_expires_at% | %lobbyaddon_suffix_expires_at%");
        CommandMessages.helpInfo(sender, "\u51b7\u5374\u79d2\u6570", "%lobbyaddon_nick_cooldown% | %lobbyaddon_prefix_cooldown% | %lobbyaddon_suffix_cooldown%");
        CommandMessages.helpFooter(sender);
        return true;
    }

    private boolean pending(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lobbyaddon.approval.review")) {
            CommandMessages.noPermission(sender);
            return true;
        }
        List<ProfileRequest> requests = plugin.getProfileService().getPendingRequests();
        CommandMessages.helpHeader(sender, "待审核申请");
        if (requests.isEmpty()) {
            CommandMessages.helpInfo(sender, "状态", "当前没有待审核申请");
        } else {
            int count = 0;
            for (ProfileRequest request : requests) {
                CommandMessages.helpLine(sender, request.getId().substring(0, 8) + " " + request.getRequesterName(),
                        request.getType().getConfigKey() + " = " + request.getValue());
                if (++count >= 10) {
                    break;
                }
            }
        }
        CommandMessages.helpFooter(sender);
        return true;
    }

    private boolean approve(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lobbyaddon.approval.review")) {
            CommandMessages.noPermission(sender);
            return true;
        }
        if (args.length != 2) {
            CommandMessages.usage(sender, "/lobbyaddon approve <审核编号>");
            return true;
        }
        ProfileRequest request = plugin.getProfileService().getRequest(args[1]);
        ProfileOperationResult result = plugin.getProfileService().approve(request, senderId(sender), sender.getName());
        if (result.getStatus() == ProfileOperationResult.Status.INVALID_VALUE) {
            CommandMessages.error(sender, "\u7533\u8bf7\u5df2\u4e0d\u7b26\u5408\u5f53\u524d\u89c4\u5219: " + result.getMessage());
            return true;
        }
        if (result.getStatus() == ProfileOperationResult.Status.CONTEXT_BLOCKED) {
            CommandMessages.error(sender, "\u7533\u8bf7\u6240\u5728\u7684\u4e16\u754c\u6216\u5b50\u670d\u52a1\u5668\u5f53\u524d\u4e0d\u5141\u8bb8\u5ba1\u6279\u3002");
            return true;
        }
        if (result.getStatus() != ProfileOperationResult.Status.APPLIED
                && result.getStatus() != ProfileOperationResult.Status.REQUEST_UNAVAILABLE) {
            CommandMessages.error(sender, "\u8be5\u7533\u8bf7\u5f53\u524d\u65e0\u6cd5\u6279\u51c6\u3002");
            return true;
        }
        if (result.getStatus() == ProfileOperationResult.Status.REQUEST_UNAVAILABLE) {
            CommandMessages.error(sender, "未找到有效的审核申请。请使用完整编号，或确保编号唯一。");
            return true;
        }
        CommandMessages.success(sender, "已批准 " + request.getRequesterName() + " 的 "
                + request.getType().getConfigKey() + " 申请。");
        Player requester = Bukkit.getPlayer(request.getRequesterId());
        if (requester != null) {
            CommandMessages.success(requester, "你的样式申请已获批准。");
        }
        return true;
    }

    private boolean reject(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lobbyaddon.approval.review")) {
            CommandMessages.noPermission(sender);
            return true;
        }
        if (args.length != 2) {
            CommandMessages.usage(sender, "/lobbyaddon reject <审核编号>");
            return true;
        }
        ProfileRequest request = plugin.getProfileService().getRequest(args[1]);
        if (!plugin.getProfileService().reject(request, senderId(sender), sender.getName())) {
            CommandMessages.error(sender, "未找到有效的审核申请。请使用完整编号，或确保编号唯一。");
            return true;
        }
        CommandMessages.success(sender, "已驳回 " + request.getRequesterName() + " 的申请。");
        Player requester = Bukkit.getPlayer(request.getRequesterId());
        if (requester != null) {
            CommandMessages.error(requester, "你的样式申请未获批准。");
        }
        return true;
    }

    private boolean history(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lobbyaddon.history")) {
            CommandMessages.noPermission(sender);
            return true;
        }
        if (args.length != 2) {
            CommandMessages.usage(sender, "/lobbyaddon history <玩家>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        List<AuditEntry> entries = plugin.getProfileService().getHistory(target.getUniqueId(), 8);
        String targetName = target.getName() == null ? args[1] : target.getName();
        CommandMessages.helpHeader(sender, targetName + " 的修改记录");
        if (entries.isEmpty()) {
            CommandMessages.helpInfo(sender, "状态", "未找到修改记录");
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.ROOT);
            for (AuditEntry entry : entries) {
                String value = entry.getValue() == null ? "重置" : entry.getValue();
                String actorName = entry.getActorName() == null ? "system" : entry.getActorName();
                CommandMessages.helpLine(sender, dateFormat.format(new Date(entry.getTimestamp())),
                        actorName + " " + entry.getAction() + " "
                                + entry.getType().getConfigKey() + " = " + value);
            }
        }
        CommandMessages.helpFooter(sender);
        return true;
    }

    private boolean menu(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            CommandMessages.playersOnly(sender);
            return true;
        }
        if (!sender.hasPermission("lobbyaddon.cosmetics.use")) {
            CommandMessages.noPermission(sender);
            return true;
        }
        plugin.getPersonalMenuService().openMain((Player) sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        CommandMessages.helpHeader(sender, "管理命令");
        if (sender.hasPermission("lobbyaddon.reload")) CommandMessages.helpLine(sender, "/lobbyaddon reload", "重新加载配置与服务");
        if (sender.hasPermission("lobbyaddon.info")) CommandMessages.helpLine(sender, "/lobbyaddon info", "查看运行状态");
        if (sender.hasPermission("lobbyaddon.debug")) CommandMessages.helpLine(sender, "/lobbyaddon debug", "查看诊断摘要");
        if (sender.hasPermission("lobbyaddon.approval.review")) {
            CommandMessages.helpLine(sender, "/lobbyaddon pending", "查看待审核申请");
            CommandMessages.helpLine(sender, "/lobbyaddon approve <编号>", "批准样式申请");
            CommandMessages.helpLine(sender, "/lobbyaddon reject <编号>", "驳回样式申请");
        }
        if (sender.hasPermission("lobbyaddon.history")) CommandMessages.helpLine(sender, "/lobbyaddon history <玩家>", "查看最近修改记录");
        if (sender.hasPermission("lobbyaddon.cosmetics.use")) CommandMessages.helpLine(sender, "/lobbyaddon menu", "打开个人设置菜单");
        if (sender.hasPermission("lobbyaddon.papi")) {
            CommandMessages.helpLine(sender, "/lobbyaddon papi", "\u67e5\u770b\u5168\u90e8 PlaceholderAPI \u53d8\u91cf");
        }
        CommandMessages.helpFooter(sender);
    }

    private static UUID senderId(CommandSender sender) {
        return sender instanceof Player ? ((Player) sender).getUniqueId() : null;
    }

    private static void addAllowed(CommandSender sender, List<String> suggestions, String value, String permission) {
        if (sender.hasPermission(permission)) {
            suggestions.add(value);
        }
    }
}
