package cn.mx.lobbyaddon.profile;

import cn.mx.lobbyaddon.LobbyAddon;
import cn.mx.lobbyaddon.config.LobbyConfig;
import cn.mx.lobbyaddon.service.NickManager;
import cn.mx.lobbyaddon.util.BukkitCompat;
import cn.mx.lobbyaddon.util.ProfileTextRules;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Shared validation for commands, GUI presets and approved requests. */
public final class ProfileValidator {

    private final LobbyAddon plugin;
    private final NickManager nickManager;

    public ProfileValidator(LobbyAddon plugin, NickManager nickManager) {
        this.plugin = plugin;
        this.nickManager = nickManager;
    }

    public String validate(Player actor, ProfileType type, String rawValue, UUID targetId) {
        LobbyConfig config = plugin.getLobbyConfig();
        return validate(type, rawValue, targetId,
                actor != null && hasColorPermission(actor, type, config),
                actor != null && canBypass(actor, type, config));
    }

    public String validateSubmitted(ProfileRequest request) {
        return validate(request.getType(), request.getValue(), request.getRequesterId(),
                request.isColorAllowed(), request.isLengthBypassAllowed());
    }

    private String validate(ProfileType type, String rawValue, UUID targetId,
                            boolean colorAllowed, boolean lengthBypassAllowed) {
        LobbyConfig config = plugin.getLobbyConfig();
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return "样式内容不能为空。";
        }
        int maximumRawLength = Math.max(16, plugin.getConfig().getInt("profile-rules.max-raw-length", 128));
        if (ProfileTextRules.exceedsRawLength(rawValue, maximumRawLength)) {
            return "样式原始内容不能超过 " + maximumRawLength + " 个字符。";
        }
        if (containsControlCharacter(rawValue)) {
            return "样式内容包含不允许的控制字符。";
        }
        if (ProfileTextRules.containsInvisibleCharacter(rawValue)) {
            return "样式内容包含不允许的不可见字符。";
        }
        if (ProfileTextRules.usesColorCode(rawValue, config.colorCode) && !config.colorEnabled) {
            return "\u989c\u8272\u4ee3\u7801\u529f\u80fd\u5f53\u524d\u672a\u542f\u7528\u3002";
        }
        if (ProfileTextRules.usesColorCode(rawValue, config.colorCode) && !colorAllowed) {
            return "你没有使用颜色代码的权限。";
        }
        String plain = nickManager.stripColor(nickManager.colorize(rawValue));
        if (type == ProfileType.NICK) {
            if (!lengthBypassAllowed) {
                if (plain.length() < config.nickMinLength) return "昵称长度不能少于 " + config.nickMinLength + " 个字符。";
                if (plain.length() > config.nickMaxLength) return "昵称长度不能超过 " + config.nickMaxLength + " 个字符。";
            }
            UUID existing = plugin.getProfileService().findUuidByNickname(plain);
            if (existing != null && !existing.equals(targetId)) {
                Player owner = Bukkit.getPlayer(existing);
                return "该昵称已被 " + (owner == null ? "其他玩家" : owner.getName()) + " 使用。";
            }
            for (Player online : BukkitCompat.getOnlinePlayers()) {
                if (online.getName().equalsIgnoreCase(plain) && !online.getUniqueId().equals(targetId)) {
                    return "不能使用其他玩家的真实名称作为昵称。";
                }
            }
            return null;
        }
        if (type == ProfileType.PREFIX) {
            if (!lengthBypassAllowed) {
                if (plain.length() < config.customMinLength) return "称号长度不能少于 " + config.customMinLength + " 个字符。";
                if (plain.length() > config.customMaxLength) return "称号长度不能超过 " + config.customMaxLength + " 个字符。";
            }
            return containsBannedWord(plain, config.bannedWords, "称号");
        }
        if (!lengthBypassAllowed) {
            if (plain.length() < config.suffixMinLength) return "后缀长度不能少于 " + config.suffixMinLength + " 个字符。";
            if (plain.length() > config.suffixMaxLength) return "后缀长度不能超过 " + config.suffixMaxLength + " 个字符。";
        }
        return containsBannedWord(plain, config.suffixBannedWords, "后缀");
    }

    private static boolean containsControlCharacter(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index)) && value.charAt(index) != '\u00a7') {
                return true;
            }
        }
        return false;
    }

    private static boolean hasColorPermission(Player player, ProfileType type, LobbyConfig config) {
        if (type == ProfileType.NICK) return player.hasPermission(config.nickColor);
        if (type == ProfileType.PREFIX) return player.hasPermission(config.customColor);
        return player.hasPermission(config.suffixColor);
    }

    private static boolean canBypass(Player player, ProfileType type, LobbyConfig config) {
        if (player == null) {
            return false;
        }
        if (type == ProfileType.NICK) return player.hasPermission(config.nickBypass);
        if (type == ProfileType.PREFIX) return player.hasPermission(config.customBypass);
        return player.hasPermission(config.suffixBypass);
    }

    private static String containsBannedWord(String value, java.util.List<String> bannedWords, String label) {
        String lower = ProfileTextRules.normalizeForComparison(value);
        for (String banned : bannedWords) {
            if (!banned.isEmpty() && lower.contains(ProfileTextRules.normalizeForComparison(banned))) {
                return label + "中包含禁用词: " + banned;
            }
        }
        return null;
    }
}
