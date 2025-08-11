package me.itstautvydas.modernwaystones;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Utils {
    public static Component formatString(String string) {
        return MiniMessage.miniMessage().deserialize(string);
    }

    public static List<Component> formatStringList(List<String> strings, boolean isLore) {
        List<Component> components = new ArrayList<>();
        for (String string : strings) {
            Component component = formatString(string);
            if (isLore)
                component = component.decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY);
            components.add(component);
        }
        return components;
    }

    public static Component formatItemName(String itemName) {
        return MiniMessage.miniMessage().deserialize(itemName).decoration(TextDecoration.ITALIC, false);
    }

    public static boolean isBedrockPlayer(UUID uuid) {
        if (!ModernWaystones.isFloodgateRunning())
            return false;
        return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
    }

    public static void loadChunkIfNeeded(Location location) {
        Chunk chunk = location.getChunk();
        if (!chunk.isLoaded())
            chunk.load();
    }

    public static String formatAgoTime(long millis, boolean inverse) {
        long now = System.currentTimeMillis();
        long diffSeconds;
        if (inverse)
            diffSeconds = (millis - now) / 1000; // time until
        else
            diffSeconds = (now - millis) / 1000; // time ago
        if (diffSeconds < 0)
            return "0s";
        long days = diffSeconds / 86400;
        long hours = (diffSeconds % 86400) / 3600;
        long minutes = (diffSeconds % 3600) / 60;
        long seconds = diffSeconds % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d");
            if (hours > 0) sb.append(" ").append(hours).append("h");
        } else if (hours > 0) {
            sb.append(hours).append("h");
            if (minutes > 0) sb.append(" ").append(minutes).append("m");
        } else if (minutes > 0) {
            sb.append(minutes).append("m");
            if (seconds > 0) sb.append(" ").append(seconds).append("s");
        } else {
            sb.append(seconds).append("s");
        }
        return sb.toString();
    }

    public static void printStackTrace(ModernWaystones plugin, Exception ex) {
        boolean showAll = plugin.getConfig().getBoolean("DataConfigurations.ShowFullExceptions");
        int exceptionsCount = 0;
        plugin.getLogger().severe(ex.getClass().getName() + ": " + ex.getMessage());
        for (StackTraceElement element : ex.getStackTrace())
            // Just in case show 3 first stack trace lines.
            if (exceptionsCount++ < 3 || showAll || element.getClassName().startsWith("me.itstautvydas"))
                plugin.getLogger().severe(element.toString());
    }
}