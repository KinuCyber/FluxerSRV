/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.fluxer.srv.utils;

import java.io.InputStream;
import java.net.URI;
import java.util.Scanner;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class UpdateChecker {
    private final JavaPlugin plugin;
    private final int resourceId;

    public UpdateChecker(JavaPlugin plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void getVersion(Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            try (InputStream inputStream = URI.create("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId).toURL().openStream();
                 Scanner scanner = new Scanner(inputStream);){
                if (scanner.hasNext()) {
                    consumer.accept(scanner.next());
                }
            }
            catch (Exception exception) {
                this.plugin.getLogger().info("Unable to check for updates: " + exception.getMessage());
            }
        });
    }
}

