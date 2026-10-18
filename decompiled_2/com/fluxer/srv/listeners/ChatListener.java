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

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String mainChannelId = this.plugin.getMainChannelId();
        if (mainChannelId == null || mainChannelId.isEmpty() || mainChannelId.equals("000000000000000000")) {
            return;
        }
        String rawMessage = event.getMessage();
        if (this.plugin.getConfig().getBoolean("fluxer-global-only", true)) {
            if (!rawMessage.startsWith("!")) {
                return;
            }
            rawMessage = rawMessage.substring(1).trim();
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

