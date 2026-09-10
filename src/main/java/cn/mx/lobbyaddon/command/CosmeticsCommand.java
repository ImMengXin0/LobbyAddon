package cn.mx.lobbyaddon.command;

import cn.mx.lobbyaddon.LobbyAddon;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CosmeticsCommand implements CommandExecutor {

    private final LobbyAddon plugin;

    public CosmeticsCommand(LobbyAddon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            CommandMessages.playersOnly(sender);
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("lobbyaddon.cosmetics.use")) {
            CommandMessages.noPermission(player);
            return true;
        }
        if (!plugin.getPresetMenuService().open(player)) {
            CommandMessages.error(player, "预设样式菜单当前未启用。");
        }
        return true;
    }
}
