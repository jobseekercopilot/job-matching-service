package com.jobseekercopilot.jobmatching.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class CommuteReferenceTimePolicy {
    static final ZoneId ZONE = ZoneId.of("Europe/London");
    private final Clock clock;

    public CommuteReferenceTimePolicy() {
        this(Clock.system(ZONE));
    }

    CommuteReferenceTimePolicy(Clock clock) {
        this.clock = clock;
    }

    public Instant nextWorkdayDeparture() {
        LocalDate date = LocalDate.now(clock).plusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date.atTime(LocalTime.of(8, 30)).atZone(ZONE).toInstant();
    }
}
