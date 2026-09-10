package cn.mx.lobbyaddon.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MenuPlaceholderTest {

    @Test
    void preservesUnderscoresInServerNames() {
        assertEquals("mini_game_1", MenuPlaceholder.onlineServer("%lobbyaddon_online_mini_game_1%"));
        assertEquals("survival", MenuPlaceholder.onlineServer("%nick_online_survival%"));
    }

    @Test
    void rejectsNonServerPlaceholders() {
        assertNull(MenuPlaceholder.onlineServer("%lobbyaddon_online%"));
    }
}
