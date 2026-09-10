package cn.mx.lobbyaddon.chat;

import java.util.Collections;
import java.util.Set;

public final class ChatStylePolicy {

    private ChatStylePolicy() {
    }

    public static ChatStyle filter(ChatStyle stored, boolean baseAllowed, boolean legacyAllowed,
                                   boolean hexAllowed, boolean gradientAllowed, boolean formatsEnabled,
                                   Set<ChatFormat> allowedFormats, boolean obfuscatedAllowed) {
        if (!baseAllowed) {
            return ChatStyle.none();
        }
        ChatStyle effective = stored == null ? ChatStyle.none() : stored;
        if ((effective.getMode() == ChatStyle.Mode.LEGACY && !legacyAllowed)
                || (effective.getMode() == ChatStyle.Mode.HEX && !hexAllowed)
                || (effective.getMode() == ChatStyle.Mode.GRADIENT && !gradientAllowed)) {
            effective = effective.withoutColor();
        }

        Set<ChatFormat> permitted = allowedFormats == null ? Collections.emptySet() : allowedFormats;
        for (ChatFormat format : ChatFormat.values()) {
            boolean allowed = formatsEnabled && permitted.contains(format)
                    && (format != ChatFormat.OBFUSCATED || obfuscatedAllowed);
            if (!allowed && effective.hasFormat(format)) {
                effective = effective.withFormat(format, false);
            }
        }
        return effective;
    }
}
