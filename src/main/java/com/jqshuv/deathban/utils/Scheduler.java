package com.jqshuv.deathban.utils;

import com.jqshuv.deathban.DeathBan;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Scheduler {

    private static final boolean IS_FOLIA = isFoliaCheck();

    private static boolean isFoliaCheck() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionScheduler");
            return true;
        } catch (ClassNotFoundException ignored) {}
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            return true;
        } catch (ClassNotFoundException ignored) {}
        return Bukkit.getVersion().contains("Folia");
    }

    public static void runDelayed(Player player, Runnable runnable, long delayTicks) {
        if (IS_FOLIA) {
            try {
                // Try to get the EntityScheduler from the player
                Object scheduler = player.getClass().getMethod("getScheduler").invoke(player);
                // Folia EntityScheduler.runDelayed signature:
                // ScheduledTask runDelayed(Plugin plugin, Consumer<ScheduledTask> task,
                // Runnable retired, long delayTicks)
                scheduler.getClass()
                        .getMethod("runDelayed", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class,
                                Runnable.class, long.class)
                        .invoke(scheduler, DeathBan.getInstance(),
                                (java.util.function.Consumer<Object>) task -> runnable.run(), null, delayTicks);
                return; // Success
            } catch (Exception e) {
                DeathBan.getInstance().getLogger()
                        .warning("Folia detected but failed to use EntityScheduler: " + e.getMessage());
                // If it fails, we don't fallback to Bukkit scheduler on Folia because it WILL
                // throw UnsupportedOperationException
                // Instead, we run it immediately or log a severe error
                DeathBan.getInstance().getLogger()
                        .severe("COULD NOT SCHEDULE TASK ON FOLIA! Running immediately as fallback.");
                runnable.run();
                return;
            }
        }

        // Fallback for Spigot/Paper
        Bukkit.getScheduler().runTaskLater(DeathBan.getInstance(), runnable, delayTicks);
    }

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public static void kick(Player player, Component reason) {
        try {
            // Try Paper/Adventure API first
            player.kick(reason);
        } catch (NoSuchMethodError | Exception e) {
            // Fallback to Spigot/Bukkit legacy string
            String legacyReason = LegacyComponentSerializer.legacySection().serialize(reason);
            player.kickPlayer(legacyReason);
        }
    }

    public static void teleport(Player player, Location location) {
        try {
            // Try Paper's teleportAsync
            java.lang.reflect.Method teleportAsync = player.getClass().getMethod("teleportAsync", Location.class);
            teleportAsync.invoke(player, location);
        } catch (Exception e) {
            // Fallback to standard teleport
            player.teleport(location);
        }
    }
}
