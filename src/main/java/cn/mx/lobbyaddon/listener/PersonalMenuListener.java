package cn.mx.lobbyaddon.listener;

import cn.mx.lobbyaddon.LobbyAddon;
import cn.mx.lobbyaddon.chat.ChatColorParser;
import cn.mx.lobbyaddon.chat.ChatFormat;
import cn.mx.lobbyaddon.command.CommandMessages;
import cn.mx.lobbyaddon.menu.PersonalMenuHolder;
import cn.mx.lobbyaddon.menu.PersonalMenuInput;
import cn.mx.lobbyaddon.menu.PersonalMenuService;
import cn.mx.lobbyaddon.profile.ProfileType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PersonalMenuListener implements Listener {

    private final LobbyAddon plugin;

    public PersonalMenuListener(LobbyAddon plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PersonalMenuHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player) || event.getRawSlot() < 0
                || event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }
        PersonalMenuHolder holder = (PersonalMenuHolder) event.getInventory().getHolder();
        PersonalMenuHolder.Action action = holder.getAction(event.getRawSlot());
        if (action == null) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        plugin.getServer().getScheduler().runTask(plugin, () -> handle(player, holder, action));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof PersonalMenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        PersonalMenuService menu = plugin.getPersonalMenuService();
        if (menu == null) {
            return;
        }
        PersonalMenuService.PendingInput pending = menu.getPending(event.getPlayer().getUniqueId());
        if (pending == null) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        if (PersonalMenuInput.isCancel(message)) {
            if (menu.completePending(event.getPlayer().getUniqueId(), pending)) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    CommandMessages.info(event.getPlayer(), "已取消输入。");
                    returnFromInput(event.getPlayer(), pending);
                });
            }
            return;
        }
        String command = PersonalMenuInput.isSafe(message) ? menu.commandFor(pending, message) : null;
        if (command == null) {
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> CommandMessages.error(event.getPlayer(), menu.invalidHint(pending)));
            return;
        }
        if (!menu.completePending(event.getPlayer().getUniqueId(), pending)) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            menu.performPendingCommand(event.getPlayer(), pending, command);
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> returnFromInput(event.getPlayer(), pending));
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PersonalMenuService menu = plugin.getPersonalMenuService();
        if (menu != null) {
            menu.cancelPending(event.getPlayer().getUniqueId());
        }
    }

    private void handle(Player player, PersonalMenuHolder holder, PersonalMenuHolder.Action action) {
        PersonalMenuService menu = plugin.getPersonalMenuService();
        if (menu == null || !player.isOnline()) {
            return;
        }
        switch (action.getType()) {
            case OPEN_PROFILE:
                if (requireProfile(player, action.getProfileType())) {
                    menu.openProfile(player, action.getProfileType());
                }
                break;
            case OPEN_CHAT:
                if (requireChat(player)) {
                    menu.openChat(player);
                }
                break;
            case PROFILE_INPUT:
                if (menu.canUseProfileDuration(player, action.getProfileType(), action.getDurationMillis())) {
                    menu.startProfileInput(player, action.getProfileType(), action.getDurationMillis());
                } else {
                    unavailable(player);
                }
                break;
            case PROFILE_RESET:
                if (requireProfile(player, action.getProfileType())) {
                    dispatchAndRefreshProfile(player, action.getProfileType(), profileCommand(action.getProfileType()) + " reset");
                }
                break;
            case PROFILE_PRESETS:
                if (!plugin.getConfig().getBoolean("presets.enabled", true)) {
                    unavailable(player);
                } else if (requireProfile(player, action.getProfileType())) {
                    plugin.getPresetMenuService().open(player, action.getProfileType());
                }
                break;
            case BACK_MAIN:
                menu.openMain(player);
                break;
            case BACK_CHAT:
                menu.openChat(player);
                break;
            case CLOSE:
                player.closeInventory();
                break;
            case CHAT_LEGACY:
                Character color = ChatColorParser.parseLegacy(action.getValue());
                if (color == null || !menu.canUseLegacy(player, color)) {
                    unavailable(player);
                } else {
                    dispatchAndRefreshChat(player, "chatcolor legacy &" + color);
                }
                break;
            case CHAT_HEX_INPUT:
                if (menu.canUseHex(player)) {
                    menu.startHexInput(player);
                } else {
                    unavailable(player);
                }
                break;
            case CHAT_GRADIENT_INPUT:
                if (menu.canUseGradient(player)) {
                    menu.startGradientInput(player);
                } else {
                    unavailable(player);
                }
                break;
            case OPEN_CHAT_PRESETS:
                if (menu.canUseChatPresets(player)) {
                    menu.openChatPresets(player, 0);
                } else {
                    unavailable(player);
                }
                break;
            case CHAT_PRESET:
                if (menu.canUseChatPresets(player)
                        && plugin.getChatColorService().isPresetAllowed(player, action.getValue())) {
                    dispatchAndRefreshChat(player, "chatcolor preset " + action.getValue());
                } else {
                    unavailable(player);
                }
                break;
            case CHAT_FORMAT:
                ChatFormat format = ChatFormat.fromInput(action.getValue());
                if (format != null && menu.canUseFormat(player, format)) {
                    dispatchAndRefreshChat(player, "chatcolor format " + format.getKey() + " toggle");
                } else {
                    unavailable(player);
                }
                break;
            case CHAT_PREVIEW:
                if (requireChat(player)) {
                    player.performCommand("chatcolor preview");
                }
                break;
            case CHAT_RESET:
                if (requireChat(player)) {
                    dispatchAndRefreshChat(player, "chatcolor reset");
                }
                break;
            case PREVIOUS_PAGE:
                menu.openChatPresets(player, holder.getPageIndex() - 1);
                break;
            case NEXT_PAGE:
                menu.openChatPresets(player, holder.getPageIndex() + 1);
                break;
            default:
                break;
        }
    }

    private boolean requireProfile(Player player, ProfileType type) {
        if (plugin.getPersonalMenuService().canUseProfile(player, type)) {
            return true;
        }
        if (!player.hasPermission(plugin.getPersonalMenuService().profilePermission(type))) {
            CommandMessages.noPermission(player);
        } else {
            unavailable(player);
        }
        return false;
    }

    private boolean requireChat(Player player) {
        if (plugin.getPersonalMenuService().canUseChat(player)) {
            return true;
        }
        if (!plugin.getPersonalMenuService().hasChatPermission(player, "use", "lobbyaddon.chatcolor.use")) {
            CommandMessages.noPermission(player);
        } else {
            unavailable(player);
        }
        return false;
    }

    private void dispatchAndRefreshProfile(Player player, ProfileType type, String command) {
        player.performCommand(command);
        plugin.getServer().getScheduler().runTask(plugin,
                () -> plugin.getPersonalMenuService().openProfile(player, type));
    }

    private void dispatchAndRefreshChat(Player player, String command) {
        player.performCommand(command);
        plugin.getServer().getScheduler().runTask(plugin,
                () -> plugin.getPersonalMenuService().openChat(player));
    }

    private void returnFromInput(Player player, PersonalMenuService.PendingInput pending) {
        if (pending.getKind() == PersonalMenuService.InputKind.PROFILE) {
            plugin.getPersonalMenuService().openProfile(player, pending.getProfileType());
        } else {
            plugin.getPersonalMenuService().openChat(player);
        }
    }

    private static String profileCommand(ProfileType type) {
        if (type == ProfileType.NICK) {
            return "nick";
        }
        return type == ProfileType.PREFIX ? "custom" : "suffix";
    }

    private static void unavailable(Player player) {
        CommandMessages.error(player, "此菜单功能当前不可用，请检查权限、功能配置或所在服务器。");
    }
}
