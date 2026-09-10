package cn.mx.lobbyaddon.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ChatMessageRendererTest {

    @Test
    void appliesLegacyColorAndFormatting() {
        ChatStyle style = ChatStyle.legacy('a').withFormat(ChatFormat.BOLD, true);

        assertEquals("\u00a7a\u00a7lHello\u00a7r",
                ChatMessageRenderer.render("Hello", style, true, true, true));
    }

    @Test
    void appliesSolidHexColorOnModernServers() {
        ChatStyle style = ChatStyle.hex(0x12AB34).withFormat(ChatFormat.ITALIC, true);

        assertEquals(hex(0x12AB34) + "\u00a7oHi\u00a7r",
                ChatMessageRenderer.render("Hi", style, true, true, true));
    }

    @Test
    void interpolatesGradientAcrossUnicodeCodePoints() {
        ChatStyle style = ChatStyle.gradient(0xFF0000, 0x0000FF)
                .withFormat(ChatFormat.BOLD, true);

        assertEquals(hex(0xFF0000) + "\u00a7lA"
                        + hex(0x800080) + "\u00a7l\ud83d\ude00"
                        + hex(0x0000FF) + "\u00a7lB\u00a7r",
                ChatMessageRenderer.render("A\ud83d\ude00B", style, true, true, true));
    }

    @Test
    void mapsModernColorsToNearestLegacyColorOnOldServers() {
        assertEquals("\u00a7aHi\u00a7r", ChatMessageRenderer.render("Hi",
                ChatStyle.hex(0x54FE54), false, true, true));
        assertEquals("\u00a7cA\u00a7eB\u00a7r", ChatMessageRenderer.render("AB",
                ChatStyle.gradient(0xFF5555, 0xFFFF55), false, true, true));
    }

    @Test
    void canDisableModernFallbackOnOldServers() {
        assertEquals("Hi", ChatMessageRenderer.render("Hi",
                ChatStyle.hex(0x123456), false, true, false));
    }

    @Test
    void keepsAllowedFormattingWhenModernFallbackIsDisabled() {
        ChatStyle style = ChatStyle.hex(0x123456).withFormat(ChatFormat.BOLD, true);

        assertEquals("\u00a7lHi\u00a7r", ChatMessageRenderer.render("Hi",
                style, false, true, false));
    }

    @Test
    void removesPlayerSuppliedFormattingCodes() {
        String rendered = ChatMessageRenderer.render("&aHello \u00a7lWorld &#112233!",
                ChatStyle.none(), true, true, true);

        assertEquals("Hello World !", rendered);
        assertFalse(rendered.contains("\u00a7"));
    }

    @Test
    void leavesPlainMessageUntouchedWhenNoStyleIsSelected() {
        assertEquals("Hello", ChatMessageRenderer.render("Hello",
                ChatStyle.none(), true, true, true));
    }

    private static String hex(int rgb) {
        String digits = String.format("%06X", rgb);
        StringBuilder result = new StringBuilder("\u00a7x");
        for (int i = 0; i < digits.length(); i++) {
            result.append('\u00a7').append(digits.charAt(i));
        }
        return result.toString();
    }
}
