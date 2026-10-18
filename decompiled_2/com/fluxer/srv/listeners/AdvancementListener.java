/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerAchievementAwardedEvent
 *  org.bukkit.plugin.Plugin
 */
package com.fluxer.srv.listeners;

import com.fluxer.srv.FluxerSRV;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;
import org.bukkit.plugin.Plugin;

public class AdvancementListener
implements Listener {
    private final FluxerSRV plugin;

    public AdvancementListener(FluxerSRV plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.NORMAL)
    public void onAchievementDone(PlayerAchievementAwardedEvent event) {
        String title = event.getAchievement().name().replace("_", " ").toLowerCase();
        CharSequence[] words = title.split(" ");
        for (int i = 0; i < words.length; ++i) {
            if (words[i].length() <= 0) continue;
            words[i] = ((String)words[i]).substring(0, 1).toUpperCase() + ((String)words[i]).substring(1);
        }
        title = String.join((CharSequence)" ", words);
        String playerName = event.getPlayer().getName();
        String message = "**" + playerName + "** has made the achievement **[" + title + "]**!";
        String mainChannelId = this.plugin.getMainChannelId();
        if (mainChannelId != null && !mainChannelId.isEmpty() && !mainChannelId.equals("000000000000000000")) {
            this.plugin.getServer().getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
                try {
                    this.plugin.getFluxerClient().getRestApi().sendMessage(mainChannelId, message);
                }
                catch (Exception e) {
                    this.plugin.getLogger().warning("Failed to send achievement to Fluxer: " + e.getMessage());
                }
            });
        }
    }
}

