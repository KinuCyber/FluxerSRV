/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.plugin.Plugin
 */
package com.fluxer.srv.listeners;

import com.fluxer.srv.FluxerSRV;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;

public class DeathListener
implements Listener {
    private final FluxerSRV plugin;

    public DeathListener(FluxerSRV plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        String mainChannelId = this.plugin.getMainChannelId();
        if (mainChannelId == null || mainChannelId.isEmpty() || mainChannelId.equals("000000000000000000")) {
            return;
        }
        String deathMessage = event.getDeathMessage();
        if (deathMessage == null || deathMessage.isEmpty()) {
            return;
        }
        String format = this.plugin.getConfig().getString("formats.death", "**%death_message%**");
        String message = format.replace("%death_message%", deathMessage);
        this.plugin.getServer().getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            try {
                this.plugin.getFluxerClient().getRestApi().sendMessage(mainChannelId, message);
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("Failed to send death message to Fluxer: " + e.getMessage());
            }
        });
    }
}

