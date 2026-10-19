package com.fluxer.srv.listeners;

import com.fluxer.srv.FluxerSRV;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.UUID;
import java.util.Set;
import java.util.UUID;

public class PlayerCommandListener implements Listener {

    private final FluxerSRV plugin;

    public PlayerCommandListener(FluxerSRV plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String cmd = event.getMessage().toLowerCase();
        if (cmd.equals("/team chat") || cmd.startsWith("/team chat ")) {
            UUID uuid = event.getPlayer().getUniqueId();
            Set<UUID> set = plugin.getTeamChatEnabled();
            if (set.contains(uuid)) {
                set.remove(uuid);
            } else {
                set.add(uuid);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getTeamChatEnabled().remove(event.getPlayer().getUniqueId());
    }
}
