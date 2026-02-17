package com.marti.vcalendarevents.events;

import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

public class Event {

    public record EventSchedule(DayOfWeek day, LocalTime time) {
    }

    private final String id;
    private final String displayName;
    private final List<String> description;
    private final String icon;
    private final List<EventSchedule> schedules;
    private final List<Integer> countdowns;
    private final Map<String, List<String>> actions;

    public Event(String id, String displayName, List<String> description, String icon,
            List<EventSchedule> schedules, List<Integer> countdowns, Map<String, List<String>> actions) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.schedules = schedules;
        this.countdowns = countdowns;
        this.actions = actions;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public List<EventSchedule> getSchedules() {
        return schedules;
    }

    public List<Integer> getCountdowns() {
        return countdowns;
    }

    public Map<String, List<String>> getActions() {
        return actions;
    }
}
