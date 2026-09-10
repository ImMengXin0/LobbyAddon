package cn.mx.lobbyaddon.profile;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileRequestTest {

    @Test
    void retainsSubmissionPermissionSnapshot() {
        ProfileRequest request = new ProfileRequest("request", UUID.randomUUID(), "Player",
                ProfileType.PREFIX, "&aTitle", 0L, 10L, 20L, true, false);

        assertTrue(request.isColorAllowed());
        assertFalse(request.isLengthBypassAllowed());
    }
}
