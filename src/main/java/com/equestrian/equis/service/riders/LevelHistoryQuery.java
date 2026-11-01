package com.equestrian.equis.service.riders;

import com.equestrian.equis.dto.riders.LevelHistoryEntry;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.repository.riders.LevelChangeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// A rider's level history, newest first: every promotion and demotion, then the placement last.
// Shared by the trainer's Rider level (4a) and the rider app's Progress. Two queries.
@Component
public class LevelHistoryQuery {
    private final LevelChangeRepository levelChanges;
    private final AssessmentBookingRepository bookings;

    public LevelHistoryQuery(LevelChangeRepository levelChanges, AssessmentBookingRepository bookings) {
        this.levelChanges = levelChanges;
        this.bookings = bookings;
    }

    @Transactional(readOnly = true)
    public List<LevelHistoryEntry> history(Rider rider) {
        List<LevelHistoryEntry> history = new ArrayList<>(levelChanges.findHistory(rider.getId()).stream()
                .map(LevelHistoryEntry::of)
                .toList());
        // The placement is the oldest event, so it goes last. Seed riders have no member.
        if (rider.getMemberId() != null) {
            bookings.findPlacedByMemberId(rider.getMemberId())
                    .ifPresent(b -> history.add(LevelHistoryEntry.placed(b)));
        }
        return List.copyOf(history);
    }
}
