/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.core.Layout
 *  org.apache.logging.log4j.core.LogEvent
 *  org.apache.logging.log4j.core.appender.AbstractAppender
 *  org.apache.logging.log4j.core.config.plugins.Plugin
 *  org.apache.logging.log4j.core.layout.PatternLayout
 *  org.bukkit.plugin.Plugin
 */
package com.fluxer.srv.logging;

import com.fluxer.srv.FluxerSRV;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;

@Plugin(name="FluxerConsoleAppender", category="Core", elementType="appender", printObject=true)
public class FluxerConsoleAppender
extends AbstractAppender {
    private final FluxerSRV plugin;
    private final Queue<String> messageQueue = new ConcurrentLinkedQueue<String>();
    private int taskId = -1;

    public FluxerConsoleAppender(FluxerSRV plugin) {
        super("FluxerConsoleAppender", null, (Layout)FluxerConsoleAppender.createFlexiblePatternLayout(), false);
        this.plugin = plugin;
    }

    private static PatternLayout createFlexiblePatternLayout() {
        try {
            Method newBuilder = PatternLayout.class.getMethod("newBuilder", new Class[0]);
            Object builder = newBuilder.invoke(null, new Object[0]);
            Method withPattern = builder.getClass().getMethod("withPattern", String.class);
            withPattern.invoke(builder, "[%d{HH:mm:ss} %level]: %msg");
            Method build = builder.getClass().getMethod("build", new Class[0]);
            return (PatternLayout)build.invoke(builder, new Object[0]);
        }
        catch (Exception newBuilder) {
            try {
                try {
                    Method create = PatternLayout.class.getMethod("createLayout", String.class, Class.forName("org.apache.logging.log4j.core.layout.PatternSelector"), Class.forName("org.apache.logging.log4j.core.config.Configuration"), Class.forName("org.apache.logging.log4j.core.pattern.RegexReplacement"), Charset.class, Boolean.TYPE, Boolean.TYPE, String.class, String.class);
                    return (PatternLayout)create.invoke(null, "[%d{HH:mm:ss} %level]: %msg", null, null, null, null, false, false, null, null);
                }
                catch (Exception create) {
                    try {
                        Method create2 = PatternLayout.class.getMethod("createLayout", String.class, Class.forName("org.apache.logging.log4j.core.config.Configuration"), Class.forName("org.apache.logging.log4j.core.pattern.RegexReplacement"), String.class, String.class);
                        return (PatternLayout)create2.invoke(null, "[%d{HH:mm:ss} %level]: %msg", null, null, null, null);
                    }
                    catch (Exception create2) {
                        Method createDefault = PatternLayout.class.getMethod("createDefaultLayout", new Class[0]);
                        return (PatternLayout)createDefault.invoke(null, new Object[0]);
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public void append(LogEvent event) {
        String message = event.getMessage().getFormattedMessage();
        if (event.getThrown() != null) {
            message = message + "\n" + event.getThrown().getMessage();
        }
        message = message.replaceAll("\u001b\\[[;\\d]*m", "");
        this.messageQueue.add(message);
    }

    public void startTask() {
        if (this.taskId != -1) {
            return;
        }
        this.taskId = this.plugin.getServer().getScheduler().scheduleSyncRepeatingTask((org.bukkit.plugin.Plugin)this.plugin, () -> {
            if (this.messageQueue.isEmpty()) {
                return;
            }
            String consoleChannelId = this.plugin.getConsoleChannelId();
            if (consoleChannelId == null || consoleChannelId.isEmpty() || consoleChannelId.equals("000000000000000000")) {
                this.messageQueue.clear();
                return;
            }
            StringBuilder sb = new StringBuilder("```\n");
            for (int count = 0; !this.messageQueue.isEmpty() && sb.length() < 1800 && count < 50; ++count) {
                sb.append(this.messageQueue.poll()).append("\n");
            }
            sb.append("```");
            String payload = sb.toString();
            new Thread(() -> {
                try {
                    this.plugin.getFluxerClient().getRestApi().sendMessage(consoleChannelId, payload);
                }
                catch (Exception e) {
                    this.plugin.getLogger().warning("Failed to send console logs to Fluxer: " + e.getMessage());
                }
            }).start();
        }, 100L, 100L);
    }

    public void stopTask() {
        if (this.taskId != -1) {
            this.plugin.getServer().getScheduler().cancelTask(this.taskId);
            this.taskId = -1;
        }
    }
}

