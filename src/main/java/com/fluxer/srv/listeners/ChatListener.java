package com.fluxer.srv.listeners;

import com.fluxer.srv.FluxerSRV;
import com.fluxer.srv.commands.StaffChatCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final FluxerSRV plugin;

    public ChatListener(FluxerSRV plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() && !plugin.isIgnoreCancelledChatEvents()) {
            return;
        }

        String rawMessage = event.getMessage();
        String staffPrefix = plugin.getStaffChatPrefix();
        boolean isStaffMessage = false;

        if (event.getPlayer().hasPermission("fluxersrv.admin")) {
            if (rawMessage.startsWith(staffPrefix)) {
                isStaffMessage = true;
                rawMessage = rawMessage.substring(staffPrefix.length()).trim();
            } else if (plugin.getAutoStaffChat().contains(event.getPlayer().getUniqueId())) {
                isStaffMessage = true;
            }
        }

        if (isStaffMessage) {
            event.setCancelled(true);
            String finalMsg = rawMessage;
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                StaffChatCommand cmd = (StaffChatCommand) plugin.getCommand("staffchat").getExecutor();
                cmd.sendStaffChatMessage(event.getPlayer(), finalMsg);
            });
            return;
        }

        String mainChannelId = plugin.getMainChannelId();
        if (mainChannelId == null || mainChannelId.isEmpty() || mainChannelId.equals("000000000000000000")) {
            return;
        }

        if (plugin.getConfig().getBoolean("fluxer-global-only", false)) {
            if (rawMessage.startsWith("!")) {
                rawMessage = rawMessage.substring(1).trim();
            } else {
                return;
            }
        }

        String format = plugin.getConfig().getString("formats.chat", "**%player_name%**: %message%");
        String message = format
                .replace("%player_name%", event.getPlayer().getName())
                .replace("%message%", rawMessage);

        final String finalMessage = message;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getFluxerClient().getRestApi().sendMessage(mainChannelId, finalMessage);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send chat message to Fluxer: " + e.getMessage());
            }
        });
    }
}
