package cn.mx.lobbyaddon.chat;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatStyleCodecTest {

    @Test
    void restoresLegacyStyleAndFormats() {
        ChatStyle style = ChatStyleCodec.decode("legacy", "a", null,
                Arrays.asList("bold", "underline"));

        assertEquals(ChatStyle.Mode.LEGACY, style.getMode());
        assertEquals('a', style.getLegacyColor());
        assertTrue(style.hasFormat(ChatFormat.BOLD));
        assertTrue(style.hasFormat(ChatFormat.UNDERLINE));
    }

    @Test
    void restoresGradientColors() {
        ChatStyle style = ChatStyleCodec.decode("gradient", "#FF0000", "#0000FF",
                Collections.singletonList("italic"));

        assertEquals(ChatStyle.Mode.GRADIENT, style.getMode());
        assertEquals(0xFF0000, style.getPrimaryRgb());
        assertEquals(0x0000FF, style.getSecondaryRgb());
        assertTrue(style.hasFormat(ChatFormat.ITALIC));
    }

    @Test
    void fallsBackToNoColorForInvalidPersistedData() {
        ChatStyle style = ChatStyleCodec.decode("hex", "not-a-color", null,
                Collections.singletonList("bold"));

        assertEquals(ChatStyle.Mode.NONE, style.getMode());
        assertTrue(style.hasFormat(ChatFormat.BOLD));
    }

    @Test
    void serializesHexAsCanonicalUppercase() {
        ChatStyle style = ChatStyle.hex(0x12abef).withFormat(ChatFormat.STRIKETHROUGH, true);

        assertEquals("hex", ChatStyleCodec.mode(style));
        assertEquals("#12ABEF", ChatStyleCodec.primary(style));
        assertEquals(Collections.singletonList("strikethrough"), ChatStyleCodec.formats(style));
    }
}
