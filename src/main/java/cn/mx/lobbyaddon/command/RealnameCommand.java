package cn.mx.lobbyaddon.command;

import cn.mx.lobbyaddon.LobbyAddon;
import cn.mx.lobbyaddon.service.NickManager;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RealnameCommand implements CommandExecutor {
    private final LobbyAddon plugin;
    private final NickManager m;
    public RealnameCommand(LobbyAddon plugin, NickManager m) {
        this.plugin = plugin;
        this.m = m;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 0) {
            CommandMessages.usage(s, "/realname <昵称>");
            return true;
        }
        UUID u = plugin.getProfileService().findUuidByNickname(String.join(" ", a));
        if (u == null) {
            CommandMessages.error(s, "未找到使用该昵称的玩家。");
            return true;
        }
        Player t = Bukkit.getPlayer(u);
        String playerName = t != null ? t.getName() : u + "（离线）";
        String nickname = plugin.getProfileService().getEffectiveNick(u);
        CommandMessages.info(s, "昵称 \u00a7r" + m.colorize(nickname)
                + " \u00a77对应玩家: \u00a7f" + playerName);
        return true;
    }
}
