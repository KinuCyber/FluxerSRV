/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.AsyncPlayerChatEvent
 *  org.bukkit.plugin.Plugin
 */
package com.fluxer.srv.listeners;

import com.fluxer.srv.FluxerSRV;
import com.fluxer.srv.commands.StaffChatCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

public class ChatListener
implements Listener {
    private final FluxerSRV plugin;

    public ChatListener(FluxerSRV plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() && !this.plugin.isIgnoreCancelledChatEvents()) {
            return;
        }
        String rawMessage = event.getMessage();
        String staffPrefix = this.plugin.getStaffChatPrefix();
        boolean isStaffMessage = false;
        if (event.getPlayer().hasPermission("fluxersrv.admin")) {
            if (rawMessage.startsWith(staffPrefix)) {
                isStaffMessage = true;
                rawMessage = rawMessage.substring(staffPrefix.length()).trim();
            } else if (this.plugin.getAutoStaffChat().contains(event.getPlayer().getUniqueId())) {
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
        String mainChannelId = this.plugin.getMainChannelId();
        if (mainChannelId == null || mainChannelId.isEmpty() || mainChannelId.equals("000000000000000000")) {
            return;
        }
        if (this.plugin.getConfig().getBoolean("fluxer-global-only", false)) {
            if (rawMessage.startsWith("!")) {
                rawMessage = rawMessage.substring(1).trim();
            } else {
                return;
            }
        }
        String format = this.plugin.getConfig().getString("formats.chat", "**%player_name%**: %message%");
        String message = format.replace("%player_name%", event.getPlayer().getName()).replace("%message%", rawMessage);
        this.plugin.getServer().getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            try {
                this.plugin.getFluxerClient().getRestApi().sendMessage(mainChannelId, message);
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("Failed to send chat message to Fluxer: " + e.getMessage());
            }
        });
    }
}

