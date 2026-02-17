package com.marti.vcalendarevents.utils;

import com.marti.vcalendarevents.vCalendarEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class MessageUtils {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    public static String formatDuration(long seconds) {
        if (seconds < 0)
            return "---";
        if (seconds < 60)
            return seconds + "s";
        long minutes = seconds / 60;
        if (minutes < 60)
            return minutes + "ᴍ " + (seconds % 60) + "s";
        long hours = minutes / 60;
        return hours + "ʜ " + (minutes % 60) + "ᴍ";
    }

    public static void sendMessage(CommandSender sender, String path) {
        sendMessage(sender, path, null);
    }

    public static void sendMessage(CommandSender sender, String path, Replacer replacer) {
        FileConfiguration config = vCalendarEvents.getInstance().getConfigManager().getMessagesConfig();
        String prefix = config.getString("general.prefix", "");

        if (config.isList(path)) {
            List<String> lines = config.getStringList(path);
            for (String line : lines) {
                line = line.replace("%prefix%", prefix);
                if (replacer != null) {
                    line = replacer.replace(line);
                }
                sender.sendMessage(miniMessage.deserialize(line));
            }
        } else {
            String message = config.getString(path);
            if (message == null)
                return;

            message = message.replace("%prefix%", prefix);
            if (replacer != null) {
                message = replacer.replace(message);
            }
            sender.sendMessage(miniMessage.deserialize(message));
        }
    }

    public static Component getMessage(String path) {
        FileConfiguration config = vCalendarEvents.getInstance().getConfigManager().getMessagesConfig();
        String prefix = config.getString("general.prefix", "");
        String message = config.getString(path, "");
        message = message.replace("%prefix%", prefix);
        return miniMessage.deserialize(message);
    }

    public interface Replacer {
        String replace(String text);
    }
}
