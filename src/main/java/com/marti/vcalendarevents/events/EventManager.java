package com.marti.vcalendarevents.events;

import com.marti.vcalendarevents.vCalendarEvents;
import com.marti.vcalendarevents.utils.ActionExecutor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public class EventManager {

    private final vCalendarEvents plugin;
    private final List<Event> events = new ArrayList<>();
    private final ActionExecutor actionExecutor;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private org.bukkit.scheduler.BukkitTask schedulerTask;

    public EventManager(vCalendarEvents plugin) {
        this.plugin = plugin;
        this.actionExecutor = new ActionExecutor(plugin);
        loadEvents();
        startScheduler();
    }

    public void shutdown() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
        }
    }

    public void loadEvents() {
        events.clear();
        FileConfiguration config = plugin.getConfigManager().getEventsConfig();
        ConfigurationSection eventsSection = config.getConfigurationSection("events");

        if (eventsSection == null)
            return;

        for (String key : eventsSection.getKeys(false)) {
            ConfigurationSection section = eventsSection.getConfigurationSection(key);
            if (section == null)
                continue;

            String displayName = section.getString("display-name", key);
            List<String> description = section.getStringList("description");
            String icon = section.getString("icon", "PAPER");

            List<String> scheduleStrings = section.getStringList("schedule");
            List<Event.EventSchedule> schedules = new ArrayList<>();
            for (String timeStr : scheduleStrings) {
                try {
                    if (timeStr.contains(";")) {
                        String[] parts = timeStr.split(";");
                        DayOfWeek day = DayOfWeek.valueOf(parts[0].toUpperCase());
                        LocalTime time = LocalTime.parse(parts[1], timeFormatter);
                        schedules.add(new Event.EventSchedule(day, time));
                    } else {
                        // Daily
                        schedules.add(new Event.EventSchedule(null, LocalTime.parse(timeStr, timeFormatter)));
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid time format for event " + key + ": " + timeStr);
                }
            }

            List<Integer> countdowns = section.getIntegerList("countdown");

            Map<String, List<String>> actions = new HashMap<>();
            ConfigurationSection actionsSection = section.getConfigurationSection("actions");
            if (actionsSection != null) {
                for (String actionKey : actionsSection.getKeys(false)) {
                    actions.put(actionKey, actionsSection.getStringList(actionKey));
                }
            }

            events.add(new Event(key, displayName, description, icon, schedules, countdowns, actions));
        }

        plugin.getLogger().info("Loaded " + events.size() + " events.");
    }

    private void startScheduler() {
        // Initial calculation
        updateEventTargetTimes();

        schedulerTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            DayOfWeek today = now.getDayOfWeek();

            for (Event event : events) {
                for (Event.EventSchedule schedule : event.getSchedules()) {
                    // Check day constraint
                    if (schedule.day() != null && schedule.day() != today) {
                        continue;
                    }

                    // Check strict match for start
                    if (now.toLocalTime().equals(schedule.time())) {
                        runEventActions(event, "start", null);
                        updateEventTargetTimes(); // Recalculate
                    }

                    // Check countdowns
                    long secondsUntil = ChronoUnit.SECONDS.between(now.toLocalTime(), schedule.time());
                    if (secondsUntil > 0 && event.getCountdowns().contains((int) secondsUntil)) {
                        String specificKey = "countdown_" + secondsUntil;
                        if (event.getActions().containsKey(specificKey)) {
                            runEventActions(event, specificKey, String.valueOf(secondsUntil));
                        } else {
                            runEventActions(event, "countdown", String.valueOf(secondsUntil));
                        }
                    }
                }
            }
        }, 0L, 20L);
    }

    private void runEventActions(Event event, String type, String timePlaceholder) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            actionExecutor.executeActions(event, type, timePlaceholder);

            // Log to Database if it's a start event
            if (type.equals("start")) {
                int actionCount = event.getActions().getOrDefault(type, Collections.emptyList()).size();
                plugin.getDatabaseManager().logEventExecution(event.getId(), actionCount);
            }

            plugin.getLogger().info("Executed event: " + event.getId() + " - Type: " + type);
        });
    }

    public List<Event> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public List<Event> getEventsForDate(LocalDate date) {
        List<Event> result = new ArrayList<>();
        DayOfWeek day = date.getDayOfWeek();
        for (Event event : events) {
            for (Event.EventSchedule schedule : event.getSchedules()) {
                if (schedule.day() == null || schedule.day() == day) {
                    result.add(event);
                    break;
                }
            }
        }
        return result;
    }

    private final Map<String, Long> eventTargetTime = new java.util.concurrent.ConcurrentHashMap<>();

    public void updateEventTargetTimes() {
        eventTargetTime.clear();
        LocalDateTime nowDateTime = LocalDateTime.now();

        for (Event event : events) {
            long minSeconds = Long.MAX_VALUE;

            for (Event.EventSchedule schedule : event.getSchedules()) {
                LocalDateTime nextRun = getNextRunTime(nowDateTime, schedule);
                long secondsUntil = ChronoUnit.SECONDS.between(nowDateTime, nextRun);

                if (secondsUntil < minSeconds) {
                    minSeconds = secondsUntil;
                }
            }
            if (minSeconds != Long.MAX_VALUE) {
                eventTargetTime.put(event.getId(), System.currentTimeMillis() + (minSeconds * 1000));
            }
        }
    }

    public long getSecondsUntilEvent(String eventId) {
        Long target = eventTargetTime.get(eventId);
        if (target == null)
            return -1;
        long diff = target - System.currentTimeMillis();
        return diff / 1000;
    }

    private LocalDateTime getNextRunTime(LocalDateTime now, Event.EventSchedule schedule) {
        LocalDateTime next = now.with(schedule.time());
        if (schedule.day() != null) {
            // Specific day
            if (now.getDayOfWeek() != schedule.day() || now.toLocalTime().isAfter(schedule.time())) {
                next = now.with(TemporalAdjusters.next(schedule.day())).with(schedule.time());
            }
        } else {
            // Daily
            if (now.toLocalTime().isAfter(schedule.time())) {
                next = next.plusDays(1);
            }
        }
        return next;
    }

    public List<EventInstance> getNextEvents(int count) {
        LocalDateTime now = LocalDateTime.now();
        List<EventInstance> instances = new ArrayList<>();

        for (Event event : events) {
            for (Event.EventSchedule schedule : event.getSchedules()) {
                LocalDateTime next = getNextRunTime(now, schedule);
                instances.add(new EventInstance(event, next));

                // Add many more occurrences to fill the count across all events
                // 30 instances per event should be enough for any standard GUI
                LocalDateTime temp = next;
                for (int i = 0; i < 30; i++) {
                    if (schedule.day() == null) {
                        temp = temp.plusDays(1);
                    } else {
                        temp = temp.with(TemporalAdjusters.next(schedule.day()));
                    }
                    instances.add(new EventInstance(event, temp));
                }
            }
        }

        Collections.sort(instances);

        if (plugin.getConfig().getBoolean("settings.debug", false)) {
            plugin.getLogger().info("Generated " + instances.size() + " total instances. Target was " + count);
            if (!instances.isEmpty()) {
                for (int i = 0; i < Math.min(5, instances.size()); i++) {
                    EventInstance inst = instances.get(i);
                    plugin.getLogger().info("  Instance " + i + ": " + inst.event().getId() + " @ " + inst.time());
                }
            }
        }

        // Return the first 'count' instances (preserving chronological order and
        // duplicates)
        return instances.subList(0, Math.min(count, instances.size()));
    }
}
