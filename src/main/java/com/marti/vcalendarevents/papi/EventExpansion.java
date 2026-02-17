package com.marti.vcalendarevents.papi;

import com.marti.vcalendarevents.vCalendarEvents;
import com.marti.vcalendarevents.events.Event;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

public class EventExpansion extends PlaceholderExpansion {

    private final vCalendarEvents plugin;

    public EventExpansion(vCalendarEvents plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "vevents";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // persist through reloads
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.equalsIgnoreCase("next_event_name")) {
            List<com.marti.vcalendarevents.events.EventInstance> nextEvents = plugin.getEventManager().getNextEvents(1);
            if (nextEvents.isEmpty())
                return "None";
            return nextEvents.get(0).event().getDisplayName();
        }

        if (params.equalsIgnoreCase("next_event_time")) {
            List<com.marti.vcalendarevents.events.EventInstance> nextEvents = plugin.getEventManager().getNextEvents(1);
            if (nextEvents.isEmpty())
                return "None";
            return nextEvents.get(0).time().toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        }

        if (params.startsWith("countdown_")) {
            String eventId = params.substring("countdown_".length());
            // Calculate time until specific event
            // Logic needed in EventManager to get next occurrence of specific event
            return getCountdown(eventId);
        }

        return null; // Placeholder not found
    }

    private String getCountdown(String eventId) {
        // Find the event
        Event targetEvent = null;
        for (Event event : plugin.getEventManager().getEvents()) {
            if (event.getId().equalsIgnoreCase(eventId)) {
                targetEvent = event;
                break;
            }
        }

        if (targetEvent == null)
            return "Unknown Event";

        long minSeconds = Long.MAX_VALUE;

        for (Event.EventSchedule schedule : targetEvent.getSchedules()) {
            LocalDateTime nowDateTime = LocalDateTime.now();
            LocalDateTime nextRun = getNextRunTime(nowDateTime, schedule);
            long secondsUntil = ChronoUnit.SECONDS.between(nowDateTime, nextRun);

            if (secondsUntil < minSeconds) {
                minSeconds = secondsUntil;
            }
        }

        if (minSeconds == Long.MAX_VALUE)
            return "N/A";

        return formatTime(minSeconds);
    }

    private String formatTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
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
}
