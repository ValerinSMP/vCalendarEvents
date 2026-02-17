package com.marti.vcalendarevents;

import com.marti.vcalendarevents.config.ConfigManager;
import com.marti.vcalendarevents.database.DatabaseManager;
import com.marti.vcalendarevents.events.EventManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class vCalendarEvents extends JavaPlugin {

    private static vCalendarEvents instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private EventManager eventManager;

    @Override
    public void onEnable() {
        instance = this;

        // Load configurations
        configManager = new ConfigManager(this);

        // Initialize Database
        databaseManager = new DatabaseManager(this);

        // Load events
        eventManager = new EventManager(this);

        // Register Commands
        getCommand("vevents").setExecutor(new com.marti.vcalendarevents.commands.MainCommand(this));

        // Register Placeholders
        if (org.bukkit.Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.marti.vcalendarevents.papi.EventExpansion(this).register();
        }

        getLogger().info("vCalendarEvents has been enabled!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    @Override
    public void onDisable() {
        if (eventManager != null) {
            eventManager.shutdown();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("vCalendarEvents has been disabled!");
    }

    public static vCalendarEvents getInstance() {
        return instance;
    }
}
