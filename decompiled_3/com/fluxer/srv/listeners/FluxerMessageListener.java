/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.Sound
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package com.fluxer.srv.listeners;

import com.fluxer.srv.FluxerListener;
import com.fluxer.srv.FluxerSRV;
import com.fluxer.srv.lib.gson.JsonObject;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class FluxerMessageListener
implements FluxerListener {
    private final FluxerSRV plugin;

    public FluxerMessageListener(FluxerSRV plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageCreate(JsonObject message) {
        if (!(message.has("author") && message.has("channel_id") && message.has("content"))) {
            return;
        }
        JsonObject author = message.getAsJsonObject("author");
        if (author.has("bot") && author.get("bot").getAsBoolean()) {
            return;
        }
        String channelId = message.get("channel_id").getAsString();
        String content = message.get("content").getAsString();
        String username = author.get("username").getAsString();
        if (channelId.equals(this.plugin.getMainChannelId())) {
            String formattedMessage = ChatColor.translateAlternateColorCodes((char)'&', (String)("&9[Discord] &f" + username + " &7" + content));
            this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> {
                for (Player p : this.plugin.getServer().getOnlinePlayers()) {
                    p.sendMessage(formattedMessage);
                }
                this.plugin.getServer().getConsoleSender().sendMessage(formattedMessage);
            });
        } else if (channelId.equals(this.plugin.getStaffChannelId())) {
            String format = this.plugin.getConfig().getString("formats.discordStaffChat", "&8[&cStaff&8] &b[Discord] &f%player_name%&8: &f%message%");
            String mcMessage = ChatColor.translateAlternateColorCodes((char)'&', (String)format.replace("%player_name%", username).replace("%message%", content));
            this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> {
                for (Player p : this.plugin.getServer().getOnlinePlayers()) {
                    if (!p.hasPermission("fluxersrv.admin") || this.plugin.getLeftStaffChat().contains(p.getUniqueId())) continue;
                    p.sendMessage(mcMessage);
                    if (this.plugin.getNoSoundStaffChat().contains(p.getUniqueId())) continue;
                    p.playSound(p.getLocation(), Sound.valueOf((String)"NOTE_PLING"), 1.0f, 1.0f);
                }
            });
        } else if (channelId.equals(this.plugin.getConsoleChannelId())) {
            this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> {
                this.plugin.getLogger().info("Executing console command from Fluxer (" + username + "): " + content);
                this.plugin.getServer().dispatchCommand((CommandSender)this.plugin.getServer().getConsoleSender(), content);
            });
        }
    }
}

