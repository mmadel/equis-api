package com.equestrian.equis.controller.sessions;

import com.equestrian.equis.dto.sessions.DeleteResponse;
import com.equestrian.equis.dto.sessions.SaveResponse;
import com.equestrian.equis.dto.sessions.SessionTemplateCreateRequest;
import com.equestrian.equis.dto.sessions.SessionTemplateResponse;
import com.equestrian.equis.dto.sessions.SessionTemplateUpdateRequest;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.service.sessions.DeleteImpact;
import com.equestrian.equis.service.sessions.SessionTemplateInput;
import com.equestrian.equis.service.sessions.Warning;

import java.util.List;

final class SessionTemplateMapper {
    private SessionTemplateMapper() {
    }

    static SessionTemplateInput toInput(SessionTemplateCreateRequest req) {
        return new SessionTemplateInput(req.levelId(), req.dayOfWeek(), req.start(), req.end(), req.capacity(),
                req.validFrom());
    }

    static SessionTemplateInput toInput(SessionTemplateUpdateRequest req) {
        return new SessionTemplateInput(req.levelId(), req.dayOfWeek(), req.start(), req.end(), req.capacity(),
                req.validFrom());
    }

    // Reads only the id of the level, so the lazy level is never loaded.
    static SessionTemplateResponse toResponse(SessionTemplate t, int maxBookedInUpcoming, int bookingsInUpcoming) {
        Integer levelId = t.isAssessment() ? null : t.getLevel().getId().intValue();
        return new SessionTemplateResponse(t.getId(), levelId, t.getDayOfWeek(), t.getStartTime(), t.getEndTime(),
                t.getCapacity(), t.getValidFrom(), maxBookedInUpcoming, bookingsInUpcoming);
    }

    static SaveResponse toResponse(SessionTemplateResponse template, List<Warning> warnings) {
        return new SaveResponse(template, warnings.stream()
                .map(w -> new SaveResponse.Warning(w.code(), w.overlaps().stream()
                        .map(o -> new SaveResponse.Overlap(o.id(), o.start(), o.end()))
                        .toList()))
                .toList());
    }

    static DeleteResponse toResponse(DeleteImpact impact) {
        return new DeleteResponse(impact.cancelledSessions(), impact.cancelledBookings());
    }
}
