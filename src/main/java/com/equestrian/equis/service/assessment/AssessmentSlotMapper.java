package com.equestrian.equis.service.assessment;

import com.equestrian.equis.dto.assessment.AssessmentSlotDto;
import com.equestrian.equis.dto.assessment.BookingRowDto;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.assessment.BookingStatus;
import com.equestrian.equis.entity.sessions.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

// Shared by the slot list and the slot detail, so A2 and A3 never disagree on booked and open.
@Component
public class AssessmentSlotMapper {
    private final ZoneId schoolZone;

    public AssessmentSlotMapper(@Value("${app.time-zone}") ZoneId schoolZone) {
        this.schoolZone = schoolZone;
    }

    // booked is the slot's bookings that aren't cancelled. A slot that hasn't started has no results yet,
    // so there this is the same count the seat rule uses.
    public AssessmentSlotDto toDto(Session slot, long booked, Instant now) {
        return new AssessmentSlotDto(slot.getId(), slot.getSessionDate(), slot.getStartTime(), slot.getEndTime(),
                slot.getCapacity(), booked, slot.isUpcoming(now, schoolZone) && booked < slot.getCapacity());
    }

    // The same moment isWaiting and requireCanRecord use.
    public boolean isStarted(Session slot, Instant now) {
        return !slot.isUpcoming(now, schoolZone);
    }

    // Another trainer's booking shows the member's name, but not their phone or the notes.
    public BookingRowDto toRow(AssessmentBooking b, UUID trainerId, Instant now) {
        boolean mine = b.getAssessor().getId().equals(trainerId);
        Integer levelId = b.getStatus() == BookingStatus.PLACED ? b.getLevel().getId().intValue() : null;
        return new BookingRowDto(b.getId(), b.getMember().getId(), b.getMember().getName(),
                mine ? b.getMember().getPhone() : null,
                b.getAssessor().getId(), b.getAssessor().getName(), mine,
                b.displayStatus(now, schoolZone), levelId, mine ? b.getNotes() : null);
    }
}
