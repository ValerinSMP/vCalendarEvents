package com.marti.vcalendarevents.utils;

import com.marti.vcalendarevents.vCalendarEvents;
import com.marti.vcalendarevents.events.Event;
import com.marti.vcalendarevents.webhooks.WebhookManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;

public class ActionExecutor {

    private final vCalendarEvents plugin;
    private final WebhookManager webhookManager;
    private final MiniMessage miniMessage;

    public ActionExecutor(vCalendarEvents plugin) {
        this.plugin = plugin;
        this.webhookManager = new WebhookManager(plugin);
        this.miniMessage = MiniMessage.miniMessage();
    }

    public void executeActions(Event event, String type, String timePlaceholder) {
        List<String> actions = event.getActions().get(type);
        if (actions == null || actions.isEmpty())
            return;

        for (String action : actions) {
            parseAndExecute(action, event, timePlaceholder);
        }
    }

    private void parseAndExecute(String actionLine, Event event, String timePlaceholder) {
        String content = actionLine.substring(actionLine.indexOf("]") + 1).trim();
        String prefix = plugin.getConfigManager().getMessagesConfig().getString("general.prefix", "");
        content = content.replace("%time%", timePlaceholder != null ? timePlaceholder : "")
                .replace("%event_displayname%", event.getDisplayName())
                .replace("%prefix%", prefix);

        if (actionLine.startsWith("[console]")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), content);
        } else if (actionLine.startsWith("[message]")) {
            Component message = miniMessage.deserialize(content);
            Bukkit.broadcast(message);
        } else if (actionLine.startsWith("[sound]")) {
            try {
                Sound sound = Sound.valueOf(content.toUpperCase());
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.playSound(player.getLocation(), sound, 1f, 1f);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound: " + content);
            }
        } else if (actionLine.startsWith("[title]")) {
            String[] parts = content.split(";");
            String titleRaw = parts.length > 0 ? parts[0] : "";
            String subtitleRaw = parts.length > 1 ? parts[1] : "";

            Component title = miniMessage.deserialize(titleRaw);
            Component subtitle = miniMessage.deserialize(subtitleRaw);

            Title titleObj = Title.title(title, subtitle,
                    Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000)));
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.showTitle(titleObj);
            }
        } else if (actionLine.startsWith("[webhook]")) {
            webhookManager.sendWebhook(content, event.getDisplayName()); // Content is the webhook name (e.g., 'global')
        }
    }
}
