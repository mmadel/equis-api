package com.equestrian.equis.dto.overview;

import java.time.LocalDate;
import java.util.List;

// The admin Overview screen in one response. date is today in school time.
public record OverviewResponse(LocalDate date,
                               List<TodaySession> sessionsToday,
                               WeekFill week,
                               Attention attention,
                               List<ActivityItem> activity) {
}
