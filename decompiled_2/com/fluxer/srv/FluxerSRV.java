/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.core.Appender
 *  org.apache.logging.log4j.core.Logger
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.fluxer.srv;

import com.fluxer.srv.FluxerClient;
import com.fluxer.srv.listeners.AdvancementListener;
import com.fluxer.srv.listeners.ChatListener;
import com.fluxer.srv.listeners.DeathListener;
import com.fluxer.srv.listeners.FluxerMessageListener;
import com.fluxer.srv.listeners.JoinQuitListener;
import com.fluxer.srv.logging.FluxerConsoleAppender;
import com.fluxer.srv.utils.UpdateChecker;
import java.io.IOException;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class FluxerSRV
extends JavaPlugin {
    private FluxerClient fluxerClient;
    private OkHttpClient httpClient;
    private String mainChannelId;
    private String consoleChannelId;
    private FluxerConsoleAppender consoleAppender;

    public void onEnable() {
        this.saveDefaultConfig();
        String token = this.getConfig().getString("botToken");
        this.mainChannelId = this.getConfig().getString("channels.global");
        this.consoleChannelId = this.getConfig().getString("channels.console");
        if (token == null || token.isEmpty() || token.equals("Bot Token_Here")) {
            this.getLogger().severe("Bot token is not set! Please configure it in config.yml");
            this.getServer().getPluginManager().disablePlugin((Plugin)this);
            return;
        }
        this.httpClient = new OkHttpClient();
        this.fluxerClient = new FluxerClient(token, this.getLogger(), this.httpClient);
        if (this.getConfig().getBoolean("check-updates", true)) {
            int spigotResourceId = 100000;
            new UpdateChecker(this, spigotResourceId).getVersion(latestVersion -> {
                String currentVersion = this.getDescription().getVersion();
                if (!currentVersion.equalsIgnoreCase((String)latestVersion)) {
                    this.getLogger().warning("========================================");
                    this.getLogger().warning("A new version of FluxerSRV is available!");
                    this.getLogger().warning("Current: v" + currentVersion + " | Latest: v" + latestVersion);
                    this.getLogger().warning("========================================");
                }
            });
        }
        this.getServer().getPluginManager().registerEvents((Listener)new ChatListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new JoinQuitListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new DeathListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new AdvancementListener(this), (Plugin)this);

        StaffChatCommand staffChatCommand = new StaffChatCommand(this);
        this.getCommand("staffchat).setExecutor((CommandExecutor)staffChatCommand);
        this.getCommand("joinstaffChat").setExecutor((CommandExecutor)staffChatCommand);
        this.getCommand("leavestaffchat").setExecutor((commandExecutor)staffChatCommand);
        this.getCommand("togglestaffchatsounds").setExecutor((CommandExecutor)staffChatCommand);

        try {
            this.fluxerClient.setListener(new FluxerMessageListener(this));
            this.fluxerClient.connect();
            if (this.mainChannelId != null && !this.mainChannelId.isEmpty() && !this.mainChannelId.equals("000000000000000000")) {
                this.fluxerClient.getRestApi().sendMessage(this.mainChannelId, this.getConfig().getString("formats.serverStart", "\ud83d\udfe9 **Server Started**"));
            }
        }
        catch (Exception e) {
            this.getLogger().severe("Failed to connect to Fluxer Gateway: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            Logger coreLogger = (Logger)LogManager.getRootLogger();
            this.consoleAppender = new FluxerConsoleAppender(this);
            this.consoleAppender.start();
            coreLogger.addAppender((Appender)this.consoleAppender);
            this.consoleAppender.startTask();
        }
        catch (Exception e) {
            this.getLogger().warning("Failed to initialize FluxerConsoleAppender. Console logs won't be forwarded.");
            e.printStackTrace();
        }
    }

    public void onDisable() {
        if (this.consoleAppender != null) {
            this.consoleAppender.stopTask();
            this.consoleAppender.stop();
            try {
                Logger coreLogger = (Logger)LogManager.getRootLogger();
                coreLogger.removeAppender((Appender)this.consoleAppender);
            }
            catch (Exception coreLogger) {
                // empty catch block
            }
        }
        if (this.fluxerClient != null) {
            if (this.mainChannelId != null && !this.mainChannelId.isEmpty() && !this.mainChannelId.equals("000000000000000000")) {
                try {
                    this.fluxerClient.getRestApi().sendMessage(this.mainChannelId, this.getConfig().getString("formats.serverStop", "\ud83d\udfe5 **Server Stopped**"));
                }
                catch (IOException e) {
                    this.getLogger().warning("Failed to send server stop message: " + e.getMessage());
                }
            }
            this.fluxerClient.shutdown();
        }
        if (this.httpClient != null) {
            this.httpClient.dispatcher().executorService().shutdown();
            this.httpClient.connectionPool().evictAll();
        }
    }

    public FluxerClient getFluxerClient() {
        return this.fluxerClient;
    }

    public String getMainChannelId() {
        return this.mainChannelId;
    }

    public String getConsoleChannelId() {
        return this.consoleChannelId;
    }
}

