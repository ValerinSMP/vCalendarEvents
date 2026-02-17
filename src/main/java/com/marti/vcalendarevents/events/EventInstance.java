package com.marti.vcalendarevents.events;

import java.time.LocalDateTime;

/**
 * Represents a specific scheduled occurrence of an event.
 */
public record EventInstance(Event event, LocalDateTime time) implements Comparable<EventInstance> {
    @Override
    public int compareTo(EventInstance other) {
        return this.time.compareTo(other.time);
    }
}
