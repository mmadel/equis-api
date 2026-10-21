package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.dto.assessment.AdminBookingRow;
import com.equestrian.equis.dto.assessment.AdminNewMemberRequest;
import com.equestrian.equis.service.assessment.AdminBookingRowAssembler;
import com.equestrian.equis.service.assessment.MemberBookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// W2's New person (flow W-C). SecurityConfig limits /admin/** to the admin.
@RestController
@RequestMapping("/admin/members")
public class AdminMemberController {
    private final MemberBookingService booking;
    private final AdminBookingRowAssembler rows;

    public AdminMemberController(MemberBookingService booking, AdminBookingRowAssembler rows) {
        this.booking = booking;
        this.rows = rows;
    }

    // Adds the person and books their first assessment; answers with their new W2 row.
    // The booking comes back with its member, slot and assessor loaded, so the row needs no open transaction.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AdminBookingRow create(@Valid @RequestBody AdminNewMemberRequest req) {
        return rows.row(booking.registerAndBook(req));
    }
}
