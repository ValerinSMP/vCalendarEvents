package com.marti.vcalendarevents.webhooks;

import com.marti.vcalendarevents.vCalendarEvents;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class WebhookManager {

    private final vCalendarEvents plugin;

    public WebhookManager(vCalendarEvents plugin) {
        this.plugin = plugin;
    }

    public void sendWebhook(String webhookName, String eventName) {
        FileConfiguration config = plugin.getConfigManager().getWebhooksConfig();
        ConfigurationSection section = config.getConfigurationSection("webhooks." + webhookName);

        if (section == null) {
            plugin.getLogger().warning("Webhook '" + webhookName + "' not found in webhooks.yml");
            return;
        }

        String url = section.getString("url");
        if (url == null || url.equals("CHANGE_ME"))
            return;

        String jsonPayload = buildJsonPayload(section, eventName);

        CompletableFuture.runAsync(() -> {
            try {
                URL obj = URI.create(url).toURL();
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("User-Agent", "vCalendarEvents");
                con.setDoOutput(true);

                try (OutputStream os = con.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = con.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    plugin.getLogger()
                            .warning("Failed to send webhook to " + webhookName + ". Response: " + responseCode);
                }

            } catch (Exception e) {
                plugin.getLogger().warning("Error sending webhook: " + e.getMessage());
            }
        });
    }

    private String buildJsonPayload(ConfigurationSection section, String eventName) {
        StringBuilder json = new StringBuilder();
        json.append("{");

        boolean first = true;

        String username = section.getString("username");
        if (username != null && !username.isEmpty()) {
            json.append("\"username\": \"").append(escapeJson(username)).append("\"");
            first = false;
        }

        String avatarUrl = section.getString("avatar");
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            if (!first)
                json.append(",");
            json.append("\"avatar_url\": \"").append(escapeJson(avatarUrl)).append("\"");
            first = false;
        }

        String content = section.getString("content", "");
        if (!content.isEmpty()) {
            if (!first)
                json.append(",");
            json.append("\"content\": \"").append(escapeJson(content)).append("\"");
            first = false;
        }

        ConfigurationSection embedSection = section.getConfigurationSection("embed");
        if (embedSection != null) {
            if (!first)
                json.append(",");
            json.append("\"embeds\": [{");

            String title = embedSection.getString("title", "").replace("%event_name%", eventName);
            String description = embedSection.getString("description", "").replace("%event_name%", eventName);

            // Fix list description
            if (embedSection.isList("description")) {
                description = String.join("\\n", embedSection.getStringList("description")).replace("%event_name%",
                        eventName);
            }

            String color = embedSection.getString("color", "#FFFFFF").replace("#", "");
            int decimalColor;
            try {
                decimalColor = Integer.parseInt(color, 16);
            } catch (NumberFormatException e) {
                decimalColor = 16777215; // White
            }

            json.append("\"title\": \"").append(escapeJson(title)).append("\",");
            json.append("\"description\": \"").append(escapeJson(description)).append("\",");
            json.append("\"color\": ").append(decimalColor);

            String footer = embedSection.getString("footer");
            if (footer != null) {
                json.append(",\"footer\": { \"text\": \"").append(escapeJson(footer)).append("\" }");
            }

            json.append("}]");
        } else if (content.isEmpty()) {
            // Fallback if both are empty
            json.append("\"content\": \"Event Started: ").append(escapeJson(eventName)).append("\"");
        }

        json.append("}");
        return json.toString();
    }

    private String escapeJson(String input) {
        if (input == null)
            return "";
        return input.replace("\"", "\\\"").replace("\n", "\\n");
    }
}
