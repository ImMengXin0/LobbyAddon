package cn.mx.lobbyaddon.menu;

import cn.mx.lobbyaddon.profile.ProfileType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public final class PersonalMenuHolder implements InventoryHolder {

    public enum Page {
        MAIN,
        PROFILE,
        CHAT,
        CHAT_PRESETS
    }

    public enum ActionType {
        OPEN_PROFILE,
        OPEN_CHAT,
        PROFILE_INPUT,
        PROFILE_RESET,
        PROFILE_PRESETS,
        BACK_MAIN,
        BACK_CHAT,
        CLOSE,
        CHAT_LEGACY,
        CHAT_HEX_INPUT,
        CHAT_GRADIENT_INPUT,
        OPEN_CHAT_PRESETS,
        CHAT_PRESET,
        CHAT_FORMAT,
        CHAT_PREVIEW,
        CHAT_RESET,
        PREVIOUS_PAGE,
        NEXT_PAGE
    }

    private final Page page;
    private final ProfileType profileType;
    private final int pageIndex;
    private final Map<Integer, Action> actions = new HashMap<>();
    private Inventory inventory;

    public PersonalMenuHolder(Page page, ProfileType profileType, int pageIndex) {
        this.page = page;
        this.profileType = profileType;
        this.pageIndex = Math.max(0, pageIndex);
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    void put(int slot, Action action) {
        actions.put(slot, action);
    }

    public Action getAction(int slot) {
        return actions.get(slot);
    }

    public Page getPage() {
        return page;
    }

    public ProfileType getProfileType() {
        return profileType;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public static final class Action {
        private final ActionType type;
        private final ProfileType profileType;
        private final long durationMillis;
        private final String value;

        private Action(ActionType type, ProfileType profileType, long durationMillis, String value) {
            this.type = type;
            this.profileType = profileType;
            this.durationMillis = durationMillis;
            this.value = value;
        }

        public static Action simple(ActionType type) {
            return new Action(type, null, 0L, null);
        }

        public static Action profile(ActionType type, ProfileType profileType) {
            return new Action(type, profileType, 0L, null);
        }

        public static Action profileInput(ProfileType profileType, long durationMillis) {
            return new Action(ActionType.PROFILE_INPUT, profileType, durationMillis, null);
        }

        public static Action value(ActionType type, String value) {
            return new Action(type, null, 0L, value);
        }

        public ActionType getType() {
            return type;
        }

        public ProfileType getProfileType() {
            return profileType;
        }

        public long getDurationMillis() {
            return durationMillis;
        }

        public String getValue() {
            return value;
        }
    }
}
