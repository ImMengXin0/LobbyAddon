package cn.mx.lobbyaddon.listener;

import cn.mx.lobbyaddon.LobbyAddon;
import cn.mx.lobbyaddon.command.CommandMessages;
import cn.mx.lobbyaddon.menu.PresetMenuHolder;
import cn.mx.lobbyaddon.profile.ProfileOperationResult;
import cn.mx.lobbyaddon.profile.ProfileService;
import cn.mx.lobbyaddon.profile.ProfileType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class PresetMenuListener implements Listener {

    private final LobbyAddon plugin;

    public PresetMenuListener(LobbyAddon plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PresetMenuHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player) || event.getRawSlot() < 0
                || event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }
        PresetMenuHolder holder = (PresetMenuHolder) event.getInventory().getHolder();
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getWhoClicked();
        if (holder.isBackSlot(event.getRawSlot()) && holder.getReturnType() != null) {
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> plugin.getPersonalMenuService().openProfile(player, holder.getReturnType()));
            return;
        }
        PresetMenuHolder.Preset preset = holder.get(event.getRawSlot());
        if (preset == null) {
            return;
        }
        if (!preset.getPermission().isEmpty() && !player.hasPermission(preset.getPermission())) {
            CommandMessages.noPermission(player);
            return;
        }
        if (!hasUsePermission(player, preset.getType())) {
            CommandMessages.noPermission(player);
            return;
        }
        String validationError = plugin.getProfileService().validate(player, preset.getType(), preset.getValue(),
                player.getUniqueId());
        if (validationError != null) {
            CommandMessages.error(player, validationError);
            return;
        }
        ProfileOperationResult result = plugin.getProfileService().requestOrApply(player, preset.getType(),
                preset.getValue(), preset.getDurationMillis());
        if (CommandMessages.reportProfileResult(player, result)) {
            String duration = preset.getDurationMillis() > 0L
                    ? "，持续 " + ProfileService.formatDuration(preset.getDurationMillis()) : "";
            CommandMessages.success(player, "预设样式已应用" + duration + "。");
            player.closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof PresetMenuHolder) {
            event.setCancelled(true);
        }
    }

    private boolean hasUsePermission(org.bukkit.entity.Player player, ProfileType type) {
        if (type == ProfileType.NICK) {
            return player.hasPermission(plugin.getLobbyConfig().nickUse);
        }
        if (type == ProfileType.PREFIX) {
            return player.hasPermission(plugin.getLobbyConfig().customUse);
        }
        return player.hasPermission(plugin.getLobbyConfig().suffixUse);
    }
}
