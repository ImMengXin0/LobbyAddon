package cn.mx.lobbyaddon.chat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class ChatStyle {

    public enum Mode {
        NONE,
        LEGACY,
        HEX,
        GRADIENT
    }

    private final Mode mode;
    private final char legacyColor;
    private final int primaryRgb;
    private final int secondaryRgb;
    private final EnumSet<ChatFormat> formats;

    private ChatStyle(Mode mode, char legacyColor, int primaryRgb, int secondaryRgb,
                      EnumSet<ChatFormat> formats) {
        this.mode = mode;
        this.legacyColor = Character.toLowerCase(legacyColor);
        this.primaryRgb = primaryRgb & 0xFFFFFF;
        this.secondaryRgb = secondaryRgb & 0xFFFFFF;
        this.formats = formats.isEmpty() ? EnumSet.noneOf(ChatFormat.class) : EnumSet.copyOf(formats);
    }

    public static ChatStyle none() {
        return new ChatStyle(Mode.NONE, 'f', 0xFFFFFF, 0xFFFFFF,
                EnumSet.noneOf(ChatFormat.class));
    }

    public static ChatStyle legacy(char color) {
        return none().withLegacyColor(color);
    }

    public static ChatStyle hex(int rgb) {
        return none().withHexColor(rgb);
    }

    public static ChatStyle gradient(int startRgb, int endRgb) {
        return none().withGradient(startRgb, endRgb);
    }

    public ChatStyle withLegacyColor(char color) {
        char normalized = Character.toLowerCase(color);
        if ("0123456789abcdef".indexOf(normalized) < 0) {
            throw new IllegalArgumentException("Unsupported legacy color: " + color);
        }
        return new ChatStyle(Mode.LEGACY, normalized, primaryRgb, secondaryRgb, formats);
    }

    public ChatStyle withHexColor(int rgb) {
        return new ChatStyle(Mode.HEX, legacyColor, rgb, rgb, formats);
    }

    public ChatStyle withGradient(int startRgb, int endRgb) {
        return new ChatStyle(Mode.GRADIENT, legacyColor, startRgb, endRgb, formats);
    }

    public ChatStyle withoutColor() {
        return new ChatStyle(Mode.NONE, legacyColor, primaryRgb, secondaryRgb, formats);
    }

    public ChatStyle withFormat(ChatFormat format, boolean enabled) {
        if (format == null) {
            return this;
        }
        EnumSet<ChatFormat> updated = formats.isEmpty()
                ? EnumSet.noneOf(ChatFormat.class) : EnumSet.copyOf(formats);
        if (enabled) {
            updated.add(format);
        } else {
            updated.remove(format);
        }
        return new ChatStyle(mode, legacyColor, primaryRgb, secondaryRgb, updated);
    }

    public ChatStyle withoutFormats() {
        return new ChatStyle(mode, legacyColor, primaryRgb, secondaryRgb,
                EnumSet.noneOf(ChatFormat.class));
    }

    public Mode getMode() {
        return mode;
    }

    public char getLegacyColor() {
        return legacyColor;
    }

    public int getPrimaryRgb() {
        return primaryRgb;
    }

    public int getSecondaryRgb() {
        return secondaryRgb;
    }

    public Set<ChatFormat> getFormats() {
        return Collections.unmodifiableSet(formats);
    }

    public boolean hasFormat(ChatFormat format) {
        return formats.contains(format);
    }

    public boolean isEmpty() {
        return mode == Mode.NONE && formats.isEmpty();
    }
}
