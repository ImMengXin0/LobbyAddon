package cn.mx.lobbyaddon.menu;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MenuMaterialTest {

    @Test
    void resolvesModernAndLegacyPaneNamesWithoutDirectEnumFields() {
        MenuMaterial pane = MenuMaterial.stainedGlassPane((short) 7);

        assertEquals("GRAY_STAINED_GLASS_PANE", pane.resolveName(names("GRAY_STAINED_GLASS_PANE")));
        assertEquals("STAINED_GLASS_PANE", pane.resolveName(names("STAINED_GLASS_PANE")));
        assertEquals(7, pane.dataFor("STAINED_GLASS_PANE"));
        assertEquals(0, pane.dataFor("GRAY_STAINED_GLASS_PANE"));
    }

    @Test
    void mapsLegacyWoolDataToModernColoredWool() {
        MenuMaterial darkGray = MenuMaterial.wool((short) 7);
        MenuMaterial lightBlue = MenuMaterial.wool((short) 3);

        assertEquals("GRAY_WOOL", darkGray.resolveName(names("GRAY_WOOL")));
        assertEquals("LIGHT_BLUE_WOOL", lightBlue.resolveName(names("LIGHT_BLUE_WOOL")));
        assertEquals("WOOL", darkGray.resolveName(names("WOOL")));
        assertEquals(7, darkGray.dataFor("WOOL"));
    }

    @Test
    void supportsEveryRenamedMenuMaterialOnOldAndNewServers() {
        assertRenamed("PLAYER_HEAD", "SKULL_ITEM", 3);
        assertRenamed("IRON_BARS", "IRON_FENCE", 0);
        assertRenamed("CLOCK", "WATCH", 0);
        assertRenamed("PURPLE_DYE", "INK_SACK", 5);
        assertRenamed("LIGHT_BLUE_DYE", "INK_SACK", 12);
        assertRenamed("ENDER_EYE", "EYE_OF_ENDER", 0);
        assertRenamed("FIREWORK_STAR", "FIREWORK_CHARGE", 0);
        assertRenamed("COBWEB", "WEB", 0);
    }

    private static void assertRenamed(String modern, String legacy, int legacyData) {
        MenuMaterial material = MenuMaterial.renamed(modern, legacy, (short) legacyData);
        assertEquals(modern, material.resolveName(names(modern)));
        assertEquals(legacy, material.resolveName(names(legacy)));
        assertEquals(legacyData, material.dataFor(legacy));
        assertEquals(0, material.dataFor(modern));
    }

    private static HashSet<String> names(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }
}
