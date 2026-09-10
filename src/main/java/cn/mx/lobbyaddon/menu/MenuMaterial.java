package cn.mx.lobbyaddon.menu;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Set;

final class MenuMaterial {

    private static final String[] DYE_COLORS = {
            "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE",
            "YELLOW", "LIME", "PINK", "GRAY",
            "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE",
            "BROWN", "GREEN", "RED", "BLACK"
    };

    private final String modernName;
    private final String legacyName;
    private final short legacyData;

    private MenuMaterial(String modernName, String legacyName, short legacyData) {
        this.modernName = modernName;
        this.legacyName = legacyName;
        this.legacyData = legacyData;
    }

    static MenuMaterial renamed(String modernName, String legacyName, short legacyData) {
        return new MenuMaterial(modernName, legacyName, legacyData);
    }

    static MenuMaterial stainedGlassPane(short legacyData) {
        return renamed(colorName(legacyData) + "_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", legacyData);
    }

    static MenuMaterial wool(short legacyData) {
        return renamed(colorName(legacyData) + "_WOOL", "WOOL", legacyData);
    }

    ItemStack createItem() {
        String resolvedName = resolveRuntimeName();
        Material material = Material.matchMaterial(resolvedName);
        if (material == null) {
            throw new IllegalStateException("Unsupported menu material: " + modernName + " / " + legacyName);
        }
        ItemStack item = new ItemStack(material);
        short data = dataFor(resolvedName);
        if (data != 0) {
            applyLegacyData(item, data);
        }
        return item;
    }

    String resolveName(Set<String> availableNames) {
        if (availableNames.contains(modernName)) {
            return modernName;
        }
        return availableNames.contains(legacyName) ? legacyName : null;
    }

    short dataFor(String resolvedName) {
        return legacyName.equals(resolvedName) ? legacyData : 0;
    }

    private String resolveRuntimeName() {
        if (Material.matchMaterial(modernName) != null) {
            return modernName;
        }
        if (Material.matchMaterial(legacyName) != null) {
            return legacyName;
        }
        throw new IllegalStateException("Unsupported menu material: " + modernName + " / " + legacyName);
    }

    private static String colorName(short legacyData) {
        int index = legacyData;
        if (index < 0 || index >= DYE_COLORS.length) {
            throw new IllegalArgumentException("Unsupported legacy color data: " + legacyData);
        }
        return DYE_COLORS[index];
    }

    private static void applyLegacyData(ItemStack item, short data) {
        try {
            Method method = ItemStack.class.getMethod("setDurability", short.class);
            method.invoke(item, data);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to apply legacy item data " + data, exception);
        }
    }
}
