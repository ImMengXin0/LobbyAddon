package cn.mx.lobbyaddon.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ChatStyleCodec {

    private ChatStyleCodec() {
    }

    public static ChatStyle decode(String mode, String primary, String secondary, List<String> formats) {
        ChatStyle style = decodeColor(mode, primary, secondary);
        if (formats != null) {
            for (String name : formats) {
                ChatFormat format = ChatFormat.fromInput(name);
                if (format != null) {
                    style = style.withFormat(format, true);
                }
            }
        }
        return style;
    }

    private static ChatStyle decodeColor(String mode, String primary, String secondary) {
        String normalized = mode == null ? "none" : mode.trim().toLowerCase(Locale.ROOT);
        if ("legacy".equals(normalized)) {
            Character color = ChatColorParser.parseLegacy(primary);
            return color == null ? ChatStyle.none() : ChatStyle.legacy(color);
        }
        if ("hex".equals(normalized)) {
            Integer color = ChatColorParser.parseHex(primary);
            return color == null ? ChatStyle.none() : ChatStyle.hex(color);
        }
        if ("gradient".equals(normalized)) {
            Integer start = ChatColorParser.parseHex(primary);
            Integer end = ChatColorParser.parseHex(secondary);
            return start == null || end == null ? ChatStyle.none() : ChatStyle.gradient(start, end);
        }
        return ChatStyle.none();
    }

    public static String mode(ChatStyle style) {
        return style.getMode().name().toLowerCase(Locale.ROOT);
    }

    public static String primary(ChatStyle style) {
        if (style.getMode() == ChatStyle.Mode.LEGACY) {
            return String.valueOf(style.getLegacyColor());
        }
        if (style.getMode() == ChatStyle.Mode.HEX || style.getMode() == ChatStyle.Mode.GRADIENT) {
            return hex(style.getPrimaryRgb());
        }
        return null;
    }

    public static String secondary(ChatStyle style) {
        return style.getMode() == ChatStyle.Mode.GRADIENT ? hex(style.getSecondaryRgb()) : null;
    }

    public static List<String> formats(ChatStyle style) {
        List<String> names = new ArrayList<>();
        for (ChatFormat format : ChatFormat.values()) {
            if (style.hasFormat(format)) {
                names.add(format.getKey());
            }
        }
        return names;
    }

    private static String hex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }
}
