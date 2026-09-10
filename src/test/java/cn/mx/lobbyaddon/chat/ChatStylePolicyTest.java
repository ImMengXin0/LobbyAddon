package cn.mx.lobbyaddon.chat;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatStylePolicyTest {

    @Test
    void removesAStoredHexColorWhenHexAccessIsRevoked() {
        ChatStyle stored = ChatStyle.hex(0x12ABEF).withFormat(ChatFormat.BOLD, true);

        ChatStyle effective = ChatStylePolicy.filter(stored,
                true, true, false, true, true, EnumSet.allOf(ChatFormat.class), true);

        assertEquals(ChatStyle.Mode.NONE, effective.getMode());
        assertTrue(effective.hasFormat(ChatFormat.BOLD));
    }

    @Test
    void removesFormatsDisabledAfterTheStyleWasSaved() {
        ChatStyle stored = ChatStyle.legacy('a')
                .withFormat(ChatFormat.BOLD, true)
                .withFormat(ChatFormat.OBFUSCATED, true);

        ChatStyle effective = ChatStylePolicy.filter(stored,
                true, true, true, true, true, EnumSet.of(ChatFormat.BOLD), false);

        assertEquals(ChatStyle.Mode.LEGACY, effective.getMode());
        assertTrue(effective.hasFormat(ChatFormat.BOLD));
        assertFalse(effective.hasFormat(ChatFormat.OBFUSCATED));
    }

    @Test
    void removesEveryFormatWhenFormatModuleIsDisabled() {
        ChatStyle stored = ChatStyle.gradient(0xFF0000, 0x0000FF)
                .withFormat(ChatFormat.ITALIC, true);

        ChatStyle effective = ChatStylePolicy.filter(stored,
                true, true, true, true, false, EnumSet.allOf(ChatFormat.class), true);

        assertEquals(ChatStyle.Mode.GRADIENT, effective.getMode());
        assertTrue(effective.getFormats().isEmpty());
    }

    @Test
    void removesTheWholeStyleWhenBaseAccessIsRevoked() {
        ChatStyle stored = ChatStyle.hex(0x12ABEF).withFormat(ChatFormat.BOLD, true);

        ChatStyle effective = ChatStylePolicy.filter(stored,
                false, true, true, true, true, EnumSet.allOf(ChatFormat.class), true);

        assertTrue(effective.isEmpty());
    }
}
