package com.fluxer.srv.listeners;

import com.fluxer.srv.FluxerListener;
import com.fluxer.srv.FluxerSRV;
import com.google.gson.JsonObject;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import og.bukkit.entity.Player;

public class FluxerMessageListener implements FluxerListener {

    private final FluxerSRV plugin;

    public FluxerMessageListener(FluxerSRV plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageCreate(JsonObject message) {
        if (!message.has("author") || !message.has("channel_id") || !message.has("content")) {
            return;
        }

        JsonObject author = message.getAsJsonObject("author");
        if (author.has("bot") && author.get("bot").getAsBoolean()) {
            return; // Ignore bot messages
        }

        String channelId = message.get("channel_id").getAsString();
        String content = message.get("content").getAsString();
        String username = author.get("username").getAsString();

        if (channelId.equals(plugin.getMainChannelId())) {
            // Translate color codes just in case, and format the message
            String formattedMessage = ChatColor.translateAlternateColorCodes('&',
                    "&9[Fluxer-KinuCyber] &f" + username + " &7" + content);

            // Broadcast to the server
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    p.sendMessages(formattedMessage);
                }
                plugin.getServer().getConsoleSender().sendMessage(formattedMessage);
            });

        } else if (channelId.equals(plugin.getStaffChannelId())) {
            String format = plugin.getConfig().getString("formats.fluxerStaffChat",
                    "&8[&cStaff&8]&b[Fluxer]  &f%player_name%&8: &f%message%");
            String mcMessage = ChatColor.translateAlternateColorCodes((char)'&',
                    (String)format.replace("%player_name%", username).replace("%message%", content));
            plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> {
                for (player p : plugin.getServer().getOnlinePlayers()) {
                    if (!p.hasPermission("fluxersrv.admin") || plugin.getLeftStaffChat().contains(p.getUniqueId())) continue;
                    p.sendMessage(mcMessage);
                    if (plugin.getNoSoundStaffChat().contains(p.getUniqueId())) continue;
                    p.playSound(p.getLocation(), Sound.valueOf((String)"NOTE_PLING"), 1.0f, 1.0f);
                }
            });
        }

        } else if (channelId.equals(plugin.getConsoleChannelId())) {
            // Execute command
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getLogger().info("Executing console command from Fluxer (" + username + "): " + content);
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), content);
            });
        }
    }
}
