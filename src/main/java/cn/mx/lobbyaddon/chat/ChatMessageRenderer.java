package cn.mx.lobbyaddon.chat;

import java.util.Set;
import java.util.regex.Pattern;

public final class ChatMessageRenderer {

    private static final char COLOR = '\u00a7';
    private static final Pattern SERIALIZED_HEX = Pattern.compile("(?i)\\u00a7x(?:\\u00a7[0-9a-f]){6}");
    private static final Pattern INPUT_HEX = Pattern.compile("(?i)[&\\u00a7]#[0-9a-f]{6}");
    private static final Pattern LEGACY_CODE = Pattern.compile("(?i)[&\\u00a7][0-9a-fk-or]");
    private static final char[] LEGACY_CODES = "0123456789abcdef".toCharArray();
    private static final int[] LEGACY_RGB = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
            0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
            0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    private ChatMessageRenderer() {
    }

    public static String render(String message, ChatStyle style, boolean supportsHex,
                                boolean modernEnabled, boolean legacyFallback) {
        String clean = stripPlayerCodes(message);
        ChatStyle selected = style == null ? ChatStyle.none() : style;
        if (selected.isEmpty()) {
            return clean;
        }

        switch (selected.getMode()) {
            case LEGACY:
                return prefixLegacy(selected.getLegacyColor(), selected.getFormats()) + clean + COLOR + 'r';
            case HEX:
                if (!modernEnabled) {
                    return renderFormatsOnly(clean, selected.getFormats());
                }
                if (supportsHex) {
                    return serializedHex(selected.getPrimaryRgb()) + serializedFormats(selected.getFormats())
                            + clean + COLOR + 'r';
                }
                if (legacyFallback) {
                    return prefixLegacy(nearestLegacy(selected.getPrimaryRgb()), selected.getFormats())
                            + clean + COLOR + 'r';
                }
                return renderFormatsOnly(clean, selected.getFormats());
            case GRADIENT:
                if (!modernEnabled) {
                    return renderFormatsOnly(clean, selected.getFormats());
                }
                if (supportsHex || legacyFallback) {
                    return renderGradient(clean, selected, supportsHex);
                }
                return renderFormatsOnly(clean, selected.getFormats());
            case NONE:
            default:
                return renderFormatsOnly(clean, selected.getFormats());
        }
    }

    public static String stripPlayerCodes(String message) {
        if (message == null || message.isEmpty()) {
            return message == null ? "" : message;
        }
        String clean = SERIALIZED_HEX.matcher(message).replaceAll("");
        clean = INPUT_HEX.matcher(clean).replaceAll("");
        return LEGACY_CODE.matcher(clean).replaceAll("");
    }

    private static String renderGradient(String text, ChatStyle style, boolean supportsHex) {
        int count = text.codePointCount(0, text.length());
        if (count == 0) {
            return text;
        }
        StringBuilder result = new StringBuilder(text.length() + count * 18);
        int index = 0;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            double ratio = count == 1 ? 0.0D : (double) index / (double) (count - 1);
            int rgb = interpolate(style.getPrimaryRgb(), style.getSecondaryRgb(), ratio);
            if (supportsHex) {
                result.append(serializedHex(rgb));
            } else {
                result.append(COLOR).append(nearestLegacy(rgb));
            }
            result.append(serializedFormats(style.getFormats()));
            result.appendCodePoint(codePoint);
            offset += Character.charCount(codePoint);
            index++;
        }
        return result.append(COLOR).append('r').toString();
    }

    private static String renderFormatsOnly(String text, Set<ChatFormat> formats) {
        if (formats.isEmpty()) {
            return text;
        }
        return serializedFormats(formats) + text + COLOR + 'r';
    }

    private static String prefixLegacy(char color, Set<ChatFormat> formats) {
        return new StringBuilder().append(COLOR).append(color).append(serializedFormats(formats)).toString();
    }

    private static String serializedFormats(Set<ChatFormat> formats) {
        StringBuilder result = new StringBuilder(formats.size() * 2);
        for (ChatFormat format : ChatFormat.values()) {
            if (formats.contains(format)) {
                result.append(COLOR).append(format.getCode());
            }
        }
        return result.toString();
    }

    private static String serializedHex(int rgb) {
        String digits = String.format("%06X", rgb & 0xFFFFFF);
        StringBuilder result = new StringBuilder(14).append(COLOR).append('x');
        for (int i = 0; i < digits.length(); i++) {
            result.append(COLOR).append(digits.charAt(i));
        }
        return result.toString();
    }

    private static int interpolate(int start, int end, double ratio) {
        int red = interpolateChannel(start >> 16, end >> 16, ratio);
        int green = interpolateChannel(start >> 8, end >> 8, ratio);
        int blue = interpolateChannel(start, end, ratio);
        return (red << 16) | (green << 8) | blue;
    }

    private static int interpolateChannel(int start, int end, double ratio) {
        return (int) Math.round((start & 0xFF) + ((end & 0xFF) - (start & 0xFF)) * ratio);
    }

    private static char nearestLegacy(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        long bestDistance = Long.MAX_VALUE;
        int best = 15;
        for (int i = 0; i < LEGACY_RGB.length; i++) {
            int candidate = LEGACY_RGB[i];
            long dr = red - ((candidate >> 16) & 0xFF);
            long dg = green - ((candidate >> 8) & 0xFF);
            long db = blue - (candidate & 0xFF);
            long distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return LEGACY_CODES[best];
    }
}
