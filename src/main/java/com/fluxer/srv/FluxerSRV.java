package com.fluxer.srv;

import okhttp3.OkHttpClient;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.CommandExecutor;

import com.fluxer.srv.commands.StaffChatCommand;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FluxerSRV extends JavaPlugin {

    private FluxerClient fluxerClient;
    private OkHttpClient httpClient;
    private static FluxerSRV instance;

    private String mainChannelId;
    private String consoleChannelId;
    private String staffChannelId;
    private boolean ignoreCancelledChatEvents;
    private String staffChatPrefix;

    private final Set<UUID> autoStaffChat    = new HashSet<UUID>();
    private final Set<UUID> leftStaffChat    = new HashSet<UUID>();
    private final Set<UUID> noSoundStaffChat = new HashSet<UUID>();
    private final Set<UUID> teamChatEnabled  = new HashSet<UUID>();


    private com.fluxer.srv.logging.FluxerConsoleAppender consoleAppender;


    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        String token = getConfig().getString("botToken");
        mainChannelId = getConfig().getString("channels.global");
        consoleChannelId = getConfig().getString("channels.console");

        this.staffChannelId = this.getConfig().getString("channels.staff");
        this.ignoreCancelledChatEvents = this.getConfig().getBoolean("ignore-cancelled-chat-events", false);
        this.staffChatPrefix = this.getConfig().getString("staff-chat-prefix", "#");

        if (token == null || token.isEmpty() || token.equals("Bot Token_Here")) {
            getLogger().severe("Bot token is not set! Please configure it in config.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        httpClient = new OkHttpClient();
        fluxerClient = new FluxerClient(token, getLogger(), httpClient);

        // Update Checker logic
        if (getConfig().getBoolean("check-updates", true)) {
            // Using a placeholder Resource ID (e.g. 100000). The user can adjust if it
            // corresponds to an actual Spigot plugin ID.
            int spigotResourceId = 100000;
            new com.fluxer.srv.utils.UpdateChecker(this, spigotResourceId).getVersion(latestVersion -> {
                String currentVersion = this.getDescription().getVersion();
                if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                    getLogger().warning("========================================");
                    getLogger().warning("A new version of FluxerSRV is available!");
                    getLogger().warning("Current: v" + currentVersion + " | Latest: v" + latestVersion);
                    getLogger().warning("========================================");
                }
            });
        }

        // Register listeners and command executors
        getServer().getPluginManager().registerEvents(new com.fluxer.srv.listeners.ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new com.fluxer.srv.listeners.JoinQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new com.fluxer.srv.listeners.DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new com.fluxer.srv.listeners.AdvancementListener(this), this);
        getServer().getPluginManager().registerEvents(new com.fluxer.srv.listeners.PlayerCommandListener(this), this);

        StaffChatCommand staffChatCommand = new StaffChatCommand(this);
        this.getCommand("staffchat").setExecutor((CommandExecutor)staffChatCommand);
        this.getCommand("joinstaffchat").setExecutor((CommandExecutor)staffChatCommand);
        this.getCommand("leavestaffchat").setExecutor((CommandExecutor)staffChatCommand);
        this.getCommand("togglestaffchatsounds").setExecutor((CommandExecutor)staffChatCommand);

        try {
            fluxerClient.setListener(new com.fluxer.srv.listeners.FluxerMessageListener(this));
            fluxerClient.connect();
            if (mainChannelId != null && !mainChannelId.isEmpty() && !mainChannelId.equals("000000000000000000")) {
                fluxerClient.getRestApi().sendMessage(mainChannelId,
                        getConfig().getString("formats.serverStart", "🟩 **Server Started**"));
            }
        } catch (Exception e) {
            getLogger().severe("Failed to connect to Fluxer Gateway: " + e.getMessage());
            e.printStackTrace();
        }

        // Setup Console Appender
        try {
            org.apache.logging.log4j.core.Logger coreLogger = (org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager
                    .getRootLogger();
            consoleAppender = new com.fluxer.srv.logging.FluxerConsoleAppender(this);
            consoleAppender.start();
            coreLogger.addAppender(consoleAppender);
            consoleAppender.startTask();
        } catch (Exception e) {
            getLogger().warning("Failed to initialize FluxerConsoleAppender. Console logs won't be forwarded.");
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        if (consoleAppender != null) {
            consoleAppender.stopTask();
            consoleAppender.stop();
            try {
                org.apache.logging.log4j.core.Logger coreLogger = (org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager
                        .getRootLogger();
                coreLogger.removeAppender(consoleAppender);
            } catch (Exception ignored) {
            }
        }

        if (fluxerClient != null) {
            if (mainChannelId != null && !mainChannelId.isEmpty() && !mainChannelId.equals("000000000000000000")) {
                try {
                    fluxerClient.getRestApi().sendMessage(mainChannelId,
                            getConfig().getString("formats.serverStop", "🟥 **Server Stopped**"));
                } catch (IOException e) {
                    getLogger().warning("Failed to send server stop message: " + e.getMessage());
                }
            }
            fluxerClient.shutdown();
        }

        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }

    public static FluxerSRV getInstance() {
        return instance;
    }

    public FluxerClient getFluxerClient() {
        return fluxerClient;
    }

    public String getMainChannelId() {
        return mainChannelId;
    }

    public String getConsoleChannelId() {
        return consoleChannelId;
    }

    public String getStaffChannelId() {
        return this.staffChannelId;
    }

    public boolean isIgnoreCancelledChatEvents() {
        return this.ignoreCancelledChatEvents;
    }

    public String getStaffChatPrefix() {
        return this.staffChatPrefix;
    }

    public Set<UUID> getAutoStaffChat() {
        return this.autoStaffChat;
    }

    public Set<UUID> getLeftStaffChat() {
        return this.leftStaffChat;
    }

    public Set<UUID> getNoSoundStaffChat() {
        return this.noSoundStaffChat;
    }

    public Set<UUID> getTeamChatEnabled() {
        return teamChatEnabled;
    }
}
