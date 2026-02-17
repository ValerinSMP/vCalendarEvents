package com.marti.vcalendarevents.database;

import com.marti.vcalendarevents.vCalendarEvents;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final vCalendarEvents plugin;
    private Connection connection;

    public DatabaseManager(vCalendarEvents plugin) {
        this.plugin = plugin;
        initializeDatabase();
    }

    private void initializeDatabase() {
        CompletableFuture.runAsync(() -> {
            try {
                File dbFile = new File(plugin.getDataFolder(), "events.db");
                if (!dbFile.exists()) {
                    try {
                        dbFile.createNewFile();
                    } catch (Exception e) {
                        plugin.getLogger().severe("Could not create database file: " + e.getMessage());
                        return;
                    }
                }

                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

                try (Statement statement = connection.createStatement()) {
                    statement.execute("CREATE TABLE IF NOT EXISTS event_history (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "event_id TEXT NOT NULL, " +
                            "timestamp LONG NOT NULL, " +
                            "actions_executed INTEGER" +
                            ");");
                }

                plugin.getLogger().info("Database initialized successfully.");

            } catch (Exception e) {
                plugin.getLogger().severe("Database initialization failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void logEventExecution(String eventId, int actionsCount) {
        if (connection == null)
            return;

        CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO event_history(event_id, timestamp, actions_executed) VALUES(?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, eventId);
                pstmt.setLong(2, System.currentTimeMillis());
                pstmt.setInt(3, actionsCount);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to log event execution: " + e.getMessage());
            }
        });
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().warning("Error closing database connection: " + e.getMessage());
            }
        }
    }
}
