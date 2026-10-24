package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.dto.assessment.AdminBookingRow;
import com.equestrian.equis.dto.assessment.AdminNewMemberRequest;
import com.equestrian.equis.dto.assessment.UpdateMemberRequest;
import com.equestrian.equis.service.assessment.AdminBookingRowAssembler;
import com.equestrian.equis.service.assessment.MemberBookingService;
import com.equestrian.equis.service.assessment.MemberCloseService;
import com.equestrian.equis.service.assessment.MemberDetailsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// W2's New person and Edit (flow W-C). SecurityConfig limits /admin/** to the admin.
@RestController
@RequestMapping("/admin/members")
public class AdminMemberController {
    private final MemberBookingService booking;
    private final MemberDetailsService details;
    private final MemberCloseService closing;
    private final AdminBookingRowAssembler rows;

    public AdminMemberController(MemberBookingService booking, MemberDetailsService details,
                                 MemberCloseService closing, AdminBookingRowAssembler rows) {
        this.booking = booking;
        this.details = details;
        this.closing = closing;
        this.rows = rows;
    }

    // Adds the person and books their first assessment; answers with their new W2 row.
    // The booking comes back with its member, slot and assessor loaded, so the row needs no open transaction.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AdminBookingRow create(@Valid @RequestBody AdminNewMemberRequest req) {
        return rows.row(booking.registerAndBook(req));
    }

    // Fixes the name, phone or email; only the fields sent change. Answers with the person's W2 row.
    @PatchMapping("/{id}")
    AdminBookingRow update(@PathVariable UUID id, @Valid @RequestBody UpdateMemberRequest req) {
        return details.update(id, req);
    }

    // Hides a person who has nothing booked from the active lists; nothing is deleted. Answers with their W2 row.
    @PostMapping("/{id}/close")
    AdminBookingRow close(@PathVariable UUID id) {
        return closing.close(id);
    }

    // Brings a closed person back with their last assessment's status. Answers with their W2 row.
    @PostMapping("/{id}/reopen")
    AdminBookingRow reopen(@PathVariable UUID id) {
        return closing.reopen(id);
    }
}
