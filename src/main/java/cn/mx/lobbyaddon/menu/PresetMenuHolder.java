package cn.mx.lobbyaddon.menu;

import cn.mx.lobbyaddon.profile.ProfileType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public final class PresetMenuHolder implements InventoryHolder {

    private final Map<Integer, Preset> presets = new HashMap<>();
    private final ProfileType returnType;
    private int backSlot = -1;
    private Inventory inventory;

    public PresetMenuHolder() {
        this(null);
    }

    PresetMenuHolder(ProfileType returnType) {
        this.returnType = returnType;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    void put(int slot, Preset preset) {
        presets.put(slot, preset);
    }

    public Preset get(int slot) {
        return presets.get(slot);
    }

    void setBackSlot(int backSlot) {
        this.backSlot = backSlot;
    }

    public boolean isBackSlot(int slot) {
        return slot == backSlot;
    }

    public ProfileType getReturnType() {
        return returnType;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public static final class Preset {
        private final ProfileType type;
        private final String value;
        private final long durationMillis;
        private final String permission;

        Preset(ProfileType type, String value, long durationMillis, String permission) {
            this.type = type;
            this.value = value;
            this.durationMillis = durationMillis;
            this.permission = permission;
        }

        public ProfileType getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        public long getDurationMillis() {
            return durationMillis;
        }

        public String getPermission() {
            return permission;
        }
    }
}
