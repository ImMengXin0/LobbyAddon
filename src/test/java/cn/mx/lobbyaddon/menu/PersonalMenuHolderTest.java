package cn.mx.lobbyaddon.menu;

import cn.mx.lobbyaddon.profile.ProfileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PersonalMenuHolderTest {

    @Test
    void keepsPageContextAndTypedSlotActions() {
        PersonalMenuHolder holder = new PersonalMenuHolder(
                PersonalMenuHolder.Page.PROFILE, ProfileType.NICK, 0);
        PersonalMenuHolder.Action action = PersonalMenuHolder.Action.profileInput(
                ProfileType.NICK, 60L * 60L * 1000L);

        holder.put(12, action);

        assertEquals(PersonalMenuHolder.Page.PROFILE, holder.getPage());
        assertEquals(ProfileType.NICK, holder.getProfileType());
        assertEquals(PersonalMenuHolder.ActionType.PROFILE_INPUT, holder.getAction(12).getType());
        assertEquals(ProfileType.NICK, holder.getAction(12).getProfileType());
        assertEquals(60L * 60L * 1000L, holder.getAction(12).getDurationMillis());
        assertNull(holder.getAction(13));
    }

    @Test
    void keepsPresetPageNumberAndActionValue() {
        PersonalMenuHolder holder = new PersonalMenuHolder(
                PersonalMenuHolder.Page.CHAT_PRESETS, null, 2);
        holder.put(10, PersonalMenuHolder.Action.value(
                PersonalMenuHolder.ActionType.CHAT_PRESET, "ocean"));

        assertEquals(2, holder.getPageIndex());
        assertEquals("ocean", holder.getAction(10).getValue());
    }
}
