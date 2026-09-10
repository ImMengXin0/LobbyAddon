package cn.mx.lobbyaddon.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NickCommandRoutingTest {

    @Test
    void menuInputAlwaysTargetsTheCommandSender() {
        assertFalse(NickCommand.targetsOther(true, true, 2, true));
        assertFalse(NickCommand.targetsOther(true, true, 4, true));
    }

    @Test
    void regularAdminSyntaxStillTargetsOnlinePlayers() {
        assertTrue(NickCommand.targetsOther(false, true, 2, true));
        assertFalse(NickCommand.targetsOther(false, false, 2, true));
        assertFalse(NickCommand.targetsOther(false, true, 1, true));
        assertFalse(NickCommand.targetsOther(false, true, 2, false));
    }
}
