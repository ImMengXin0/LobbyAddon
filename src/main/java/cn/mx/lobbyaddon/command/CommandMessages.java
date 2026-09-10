package cn.mx.lobbyaddon.command;

import org.bukkit.command.CommandSender;
import cn.mx.lobbyaddon.profile.ProfileOperationResult;
import cn.mx.lobbyaddon.profile.ProfileService;

public final class CommandMessages {

    private static final String PREFIX = "\u00a78[\u00a7bLobbyAddon\u00a78] \u00a7r";
    private static final String LINE = "\u00a78\u00a7m--------------------------------\u00a7r";

    private CommandMessages() {
    }

    public static void success(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + "\u00a7a" + message);
    }

    public static void error(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + "\u00a7c" + message);
    }

    public static void info(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + "\u00a77" + message);
    }

    public static void usage(CommandSender sender, String syntax) {
        sender.sendMessage(PREFIX + "\u00a7e用法: \u00a7f" + syntax);
    }

    public static void helpHeader(CommandSender sender, String title) {
        sender.sendMessage(LINE);
        sender.sendMessage("\u00a7bLobbyAddon \u00a78- \u00a7f" + title);
    }

    public static void helpLine(CommandSender sender, String syntax, String description) {
        sender.sendMessage("\u00a7b" + syntax + " \u00a78- \u00a77" + description);
    }

    public static void helpInfo(CommandSender sender, String label, String value) {
        sender.sendMessage("\u00a77" + label + ": \u00a7r" + value);
    }

    public static void helpFooter(CommandSender sender) {
        sender.sendMessage(LINE);
    }

    public static void noPermission(CommandSender sender) {
        error(sender, "你没有权限执行此命令。");
    }

    public static void playersOnly(CommandSender sender) {
        error(sender, "该命令只能由玩家执行。");
    }

    public static boolean reportProfileResult(CommandSender sender, ProfileOperationResult result) {
        switch (result.getStatus()) {
            case APPLIED:
                return true;
            case PENDING_APPROVAL:
                info(sender, "你的申请已提交，审核编号: " + result.getRequestId().substring(0, 8) + "。");
                return false;
            case COOLDOWN:
                error(sender, "请等待 " + ProfileService.formatDuration(result.getRemainingMillis()) + " 后再试。");
                return false;
            case CONTEXT_BLOCKED:
                error(sender, "此功能在当前世界或子服务器不可用。");
                return false;
            case DEPENDENCY_UNAVAILABLE:
                error(sender, "未检测到 LuckPerms，无法执行此操作。");
                return false;
            case TEMPORARY_DISABLED:
                error(sender, "临时样式功能当前未启用。");
                return false;
            case STORAGE_UNAVAILABLE:
                error(sender, "昵称存储尚未就绪或当前不可用，请稍后重试。");
                return false;
            case DURATION_TOO_LONG:
                error(sender, "临时样式的持续时间超过服务器限制。");
                return false;
            case FEATURE_DISABLED:
                error(sender, "\u8be5\u6837\u5f0f\u529f\u80fd\u5f53\u524d\u672a\u542f\u7528\u3002");
                return false;
            case INVALID_VALUE:
                error(sender, result.getMessage() == null ? "\u8f93\u5165\u7684\u6837\u5f0f\u5185\u5bb9\u65e0\u6548\u3002" : result.getMessage());
                return false;
            case REQUEST_UNAVAILABLE:
                error(sender, "\u672a\u627e\u5230\u6709\u6548\u7684\u5ba1\u6838\u7533\u8bf7\u3002");
                return false;
            default:
                error(sender, "操作未完成。");
                return false;
        }
    }
}
