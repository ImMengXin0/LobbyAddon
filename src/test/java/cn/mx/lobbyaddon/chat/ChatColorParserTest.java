package cn.mx.lobbyaddon.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChatColorParserTest {

    @Test
    void acceptsLegacyCodesAndNames() {
        assertEquals(Character.valueOf('a'), ChatColorParser.parseLegacy("&a"));
        assertEquals(Character.valueOf('b'), ChatColorParser.parseLegacy("aqua"));
        assertEquals(Character.valueOf('c'), ChatColorParser.parseLegacy("red"));
        assertEquals(Character.valueOf('e'), ChatColorParser.parseLegacy("yellow"));
        assertNull(ChatColorParser.parseLegacy("rainbow"));
    }

    @Test
    void acceptsSixDigitHexColorsOnly() {
        assertEquals(Integer.valueOf(0x12ABEF), ChatColorParser.parseHex("#12ABef"));
        assertEquals(Integer.valueOf(0x12ABEF), ChatColorParser.parseHex("12ABef"));
        assertNull(ChatColorParser.parseHex("#123"));
        assertNull(ChatColorParser.parseHex("#GG0000"));
    }

    @Test
    void recognizesFormattingNames() {
        assertEquals(ChatFormat.BOLD, ChatFormat.fromInput("bold"));
        assertEquals(ChatFormat.UNDERLINE, ChatFormat.fromInput("underlined"));
        assertEquals(ChatFormat.STRIKETHROUGH, ChatFormat.fromInput("strike"));
        assertNull(ChatFormat.fromInput("large"));
    }
}
