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

    if (event.isCancelled() && !this.plugin.igIgnoreCancelledChatEvents()) {
        return;
    }

    String rawMessage = event.getMessage();
    String staffPrefix = this.plugin.getStaffChatPrefix();
    boolean isStaffMessage = false;
    if (event.getPlayer().hasPermission("fluxersrv.admin")) {
        if (rawMessage.startsWith(staffPrefix)) {
            isStaffMessage = true;
            rawMessage = rawMessage.substring(staffPrefix.length()).trim();
        } else if (this.plugin.getAutostaffChat().contains(event.getPlayer().getUniqueID())) {
            isStaffMessage = true;
        }
    }
    if (isStaffMessage) {
        event.setCancelled(true);
        String finalMsg = rawMessage;
        this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> {
            StaffChatCommand cmd = (StaffChatCommand)this.plugin.getCommand("staffchat").getExecutor();
            cmd.sendStaffChatMessage(event.getPlayer(), finalMsg);
        });
        return;
    }

    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String mainChannelId = plugin.getMainChannelId();
        if (mainChannelId == null || mainChannelId.isEmpty() || mainChannelId.equals("000000000000000000")) {
            return;
        }

        if (plugin.getConfig().getBoolean("fluxer-global-only", false)) {
            if (!rawMessage.startsWith("!")) {
                rawMessage = rawMessage.substring(1).trim();
            } else {
                return; // Local chat, DO NOT SEND to Fluxer
            }
        }

        String format = plugin.getConfig().getString("formats.chat", "**%player_name%**: %message%");
        String message = format
                .replace("%player_name%", event.getPlayer().getName())
                .replace("%message%", rawMessage);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getFluxerClient().getRestApi().sendMessage(mainChannelId, message);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send chat message to Fluxer: " + e.getMessage());
            }
        });
    }
}
