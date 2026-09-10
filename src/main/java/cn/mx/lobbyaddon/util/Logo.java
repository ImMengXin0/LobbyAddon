package cn.mx.lobbyaddon.util;

import org.bukkit.Bukkit;

/**
 * 启动 Logo — 控制台渐变 ASCII 艺术字
 */
public final class Logo {

    private Logo() {
    }

    public static void print() {
        String[] lines = {
                " _     ___  ____  ______   __ _    ____  ____   ___  _   _ ",
                "| |   / _ \\| __ )| __ ) \\ / // \\  |  _ \\|  _ \\ / _ \\| \\ | |",
                "| |  | | | |  _ \\|  _ \\\\ V // _ \\ | | | | | | | | | |  \\| |",
                "| |__| |_| | |_) | |_) || |/ ___ \\| |_| | |_| | |_| | |\\  |",
                "|_____\\___/|____/|____/ |_/_/   \\_\\____/|____/ \\___/|_| \\_|"
        };
        String[] colors = {"\u00a7c", "\u00a76", "\u00a7e", "\u00a7a", "\u00a7b"};
        for (int i = 0; i < lines.length; i++) {
            Bukkit.getConsoleSender().sendMessage(colors[i] + lines[i]);
        }
    }
}
