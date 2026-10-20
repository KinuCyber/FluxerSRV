/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.Sound
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package com.fluxer.srv.commands;

import com.fluxer.srv.FluxerSRV;
import java.io.IOException;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class StaffChatCommand
implements CommandExecutor {
    private final FluxerSRV plugin;

    public StaffChatCommand(FluxerSRV plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use staff chat commands.");
            return true;
        }
        Player player = (Player)sender;
        if (command.getName().equalsIgnoreCase("staffchat")) {
            if (args.length == 0) {
                if (this.plugin.getAutoStaffChat().contains(player.getUniqueId())) {
                    this.plugin.getAutoStaffChat().remove(player.getUniqueId());
                    player.sendMessage(ChatColor.YELLOW + "Auto staff chat disabled.");
                } else {
                    this.plugin.getAutoStaffChat().add(player.getUniqueId());
                    player.sendMessage(ChatColor.GREEN + "Auto staff chat enabled.");
                }
            } else {
                this.sendStaffChatMessage(player, String.join((CharSequence)" ", args));
            }
        } else if (command.getName().equalsIgnoreCase("joinstaffchat")) {
            this.plugin.getLeftStaffChat().remove(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "You have joined the staff chat.");
        } else if (command.getName().equalsIgnoreCase("leavestaffchat")) {
            this.plugin.getLeftStaffChat().add(player.getUniqueId());
            player.sendMessage(ChatColor.YELLOW + "You have left the staff chat.");
        } else if (command.getName().equalsIgnoreCase("togglestaffchatsounds")) {
            if (this.plugin.getNoSoundStaffChat().contains(player.getUniqueId())) {
                this.plugin.getNoSoundStaffChat().remove(player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "Staff chat sounds enabled.");
            } else {
                this.plugin.getNoSoundStaffChat().add(player.getUniqueId());
                player.sendMessage(ChatColor.YELLOW + "Staff chat sounds disabled.");
            }
        }
        return true;
    }

    public void sendStaffChatMessage(Player sender, String message) {
        String staffFormat = this.plugin.getConfig().getString("formats.staffChat", "[Staff] **%player_name%**: %message%");
        String discordMessage = staffFormat.replace("%player_name%", sender.getName()).replace("%message%", message);
        String mcFormat = this.plugin.getConfig().getString("formats.discordStaffChat", "&8[&cStaff&8] &b[Discord] &f%player_name%&8: &f%message%");
        String mcMessage = ChatColor.translateAlternateColorCodes((char)'&', (String)mcFormat.replace("%player_name%", sender.getName()).replace("%message%", message));
        String staffChannelId = this.plugin.getStaffChannelId();
        if (staffChannelId != null && !staffChannelId.isEmpty() && !staffChannelId.equals("000000000000000000")) {
            this.plugin.getServer().getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
                try {
                    this.plugin.getFluxerClient().getRestApi().sendMessage(staffChannelId, discordMessage);
                }
                catch (IOException e) {
                    this.plugin.getLogger().warning("Failed to send staff chat to Fluxer: " + e.getMessage());
                }
            });
        }
        for (Player p : this.plugin.getServer().getOnlinePlayers()) {
            if (!p.hasPermission("fluxersrv.admin") || this.plugin.getLeftStaffChat().contains(p.getUniqueId())) continue;
            p.sendMessage(mcMessage);
            if (this.plugin.getNoSoundStaffChat().contains(p.getUniqueId())) continue;
            //p.playSound(p.getLocation(), Sound.valueOf((String)"NOTE_PLING"), 1.0f, 1.0f);
        }
    }
}

