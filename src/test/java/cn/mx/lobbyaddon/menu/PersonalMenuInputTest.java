package cn.mx.lobbyaddon.menu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalMenuInputTest {

    @Test
    void recognizesChineseAndEnglishCancellationWords() {
        assertTrue(PersonalMenuInput.isCancel("取消"));
        assertTrue(PersonalMenuInput.isCancel(" cancel "));
        assertFalse(PersonalMenuInput.isCancel("取消昵称"));
    }

    @Test
    void rejectsCommandsAndControlCharacters() {
        assertFalse(PersonalMenuInput.isSafe("/op player"));
        assertFalse(PersonalMenuInput.isSafe("hello\nworld"));
        assertFalse(PersonalMenuInput.isSafe("§a#12ABEF"));
        assertFalse(PersonalMenuInput.isSafe("   "));
        assertTrue(PersonalMenuInput.isSafe("&6星光旅人"));
    }

    @Test
    void buildsPermanentAndTemporaryProfileCommands() {
        assertEquals("nick 星光旅人", PersonalMenuInput.profileCommand("nick", 0L, "星光旅人"));
        assertEquals("custom temp 30m &6VIP",
                PersonalMenuInput.profileCommand("custom", 30L * 60L * 1000L, "&6VIP"));
        assertEquals("suffix temp 1d 会员",
                PersonalMenuInput.profileCommand("suffix", 24L * 60L * 60L * 1000L, "会员"));
    }

    @Test
    void buildsHexAndGradientCommandsFromChatInput() {
        assertEquals("chatcolor hex #12ABEF", PersonalMenuInput.hexCommand(" #12abef "));
        assertEquals("chatcolor gradient #FF512F #F09819",
                PersonalMenuInput.gradientCommand("#ff512f   #f09819"));
        assertNull(PersonalMenuInput.hexCommand("red"));
        assertNull(PersonalMenuInput.gradientCommand("#FF512F"));
    }
}
