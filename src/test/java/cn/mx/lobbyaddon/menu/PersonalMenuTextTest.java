package cn.mx.lobbyaddon.menu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PersonalMenuTextTest {

    @Test
    void rendersHexProfileValuesInsteadOfShowingRawColorCodes() {
        String renderedHex = "\u00a7x\u00a71\u00a72\u00a7A\u00a7B\u00a7E\u00a7F";

        String result = PersonalMenuText.profileValue("&#12ABEF会员",
                value -> value.replace("&#12ABEF", renderedHex));

        assertEquals(renderedHex + "会员", result);
        assertFalse(result.contains("&#12ABEF"));
    }

    @Test
    void keepsTheUnsetLabelWhenNoProfileValueExists() {
        assertEquals("&8未设置", PersonalMenuText.profileValue(null, value -> "rendered:" + value));
        assertEquals("&8未设置", PersonalMenuText.profileValue("", value -> "rendered:" + value));
    }

    @Test
    void usesThePlayerNameWhenNoNicknameExists() {
        assertEquals("Steve", PersonalMenuText.profileValueOrDefault(null, "Steve",
                value -> value.replace("&#12ABEF", "colored:")));
    }
}
