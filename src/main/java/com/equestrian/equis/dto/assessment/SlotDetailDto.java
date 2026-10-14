package com.equestrian.equis.dto.assessment;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

// One slot with its bookings, cancelled ones left out. started is true from the start time itself.
public record SlotDetailDto(UUID id,
                            LocalDate date,
                            @JsonFormat(pattern = "HH:mm") LocalTime start,
                            @JsonFormat(pattern = "HH:mm") LocalTime end,
                            int capacity,
                            long booked,
                            boolean open,
                            boolean started,
                            List<BookingRowDto> bookings) {
    public static SlotDetailDto of(AssessmentSlotDto slot, boolean started, List<BookingRowDto> bookings) {
        return new SlotDetailDto(slot.id(), slot.date(), slot.start(), slot.end(), slot.capacity(), slot.booked(),
                slot.open(), started, bookings);
    }
}
