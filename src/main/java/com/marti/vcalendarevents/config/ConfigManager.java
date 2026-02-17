package com.marti.vcalendarevents.config;

import com.marti.vcalendarevents.vCalendarEvents;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final vCalendarEvents plugin;

    private FileConfiguration eventsConfig;

    private FileConfiguration webhooksConfig;
    private final File webhooksFile;

    private FileConfiguration guiConfig;
    private final File guiFile;

    private FileConfiguration messagesConfig;
    private final File messagesFile;

    public ConfigManager(vCalendarEvents plugin) {
        this.plugin = plugin;
        this.webhooksFile = new File(plugin.getDataFolder(), "webhooks.yml");
        this.guiFile = new File(plugin.getDataFolder(), "gui.yml");
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        loadConfigs();
    }

    public void loadConfigs() {
        // Load config.yml (Events & Settings)
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        eventsConfig = plugin.getConfig();

        // Load webhooks.yml
        if (!webhooksFile.exists()) {
            plugin.saveResource("webhooks.yml", false);
        }
        webhooksConfig = YamlConfiguration.loadConfiguration(webhooksFile);

        // Load gui.yml
        if (!guiFile.exists()) {
            plugin.saveResource("gui.yml", false);
        }
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);

        // Load messages.yml
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Summarize counts
        int eventCount = eventsConfig.getConfigurationSection("events") != null
                ? eventsConfig.getConfigurationSection("events").getKeys(false).size()
                : 0;
        int webhookCount = webhooksConfig.getConfigurationSection("webhooks") != null
                ? webhooksConfig.getConfigurationSection("webhooks").getKeys(false).size()
                : webhooksConfig.getKeys(false).size();
        int messageCount = messagesConfig.getKeys(false).size();
        int guiMenus = guiConfig.getKeys(false).size();

        plugin.getLogger().info("------------------------------------------");
        plugin.getLogger().info("¡Configuraciones actualizadas con éxito!");
        plugin.getLogger().info(" » Eventos del calendario: " + eventCount);
        plugin.getLogger().info(" » Webhooks operativos: " + webhookCount);
        plugin.getLogger().info(" » Mensajes traducidos: " + messageCount);
        plugin.getLogger().info(" » Menús registrados: " + guiMenus);
        plugin.getLogger().info("------------------------------------------");
    }

    public void reloadConfigs() {
        loadConfigs();
    }

    public void saveWebhooksConfig() {
        try {
            webhooksConfig.save(webhooksFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save webhooks.yml!");
            e.printStackTrace();
        }
    }

    public FileConfiguration getEventsConfig() {
        return eventsConfig;
    }

    public FileConfiguration getWebhooksConfig() {
        return webhooksConfig;
    }

    public FileConfiguration getGuiConfig() {
        return guiConfig;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
}
