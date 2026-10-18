/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 */
package com.fluxer.srv.listeners;

import com.fluxer.srv.FluxerSRV;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class JoinQuitListener
implements Listener {
    private final FluxerSRV plugin;

    public JoinQuitListener(FluxerSRV plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.sendMessage("formats.join", "**%player_name%** joined the server!", event.getPlayer().getName());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.sendMessage("formats.quit", "**%player_name%** left the server!", event.getPlayer().getName());
    }

    private void sendMessage(String formatKey, String defaultFormat, String playerName) {
        String mainChannelId = this.plugin.getMainChannelId();
        if (mainChannelId == null || mainChannelId.isEmpty() || mainChannelId.equals("000000000000000000")) {
            return;
        }
        String format = this.plugin.getConfig().getString(formatKey, defaultFormat);
        String message = format.replace("%player_name%", playerName);
        this.plugin.getServer().getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            try {
                this.plugin.getFluxerClient().getRestApi().sendMessage(mainChannelId, message);
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("Failed to send join/quit message to Fluxer: " + e.getMessage());
            }
        });
    }
}

