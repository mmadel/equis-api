package com.equestrian.equis.controller.riders;

import com.equestrian.equis.dto.riders.AdminRiderList;
import com.equestrian.equis.dto.riders.ReassignResult;
import com.equestrian.equis.dto.riders.ReassignRidersRequest;
import com.equestrian.equis.service.riders.AdminRiderService;
import com.equestrian.equis.service.riders.RiderReassignService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// W3, the admin Riders & trainers screen. SecurityConfig limits /admin/** to the admin.
@RestController
@RequestMapping("/admin/riders")
public class AdminRiderController {
    private final AdminRiderService service;
    private final RiderReassignService reassign;

    public AdminRiderController(AdminRiderService service, RiderReassignService reassign) {
        this.service = service;
        this.reassign = reassign;
    }

    // The whole list, totals and trainer cards in one answer; the filters only narrow the rows.
    @GetMapping
    AdminRiderList list(@RequestParam(required = false) Integer level, @RequestParam(required = false) UUID trainer,
                        @RequestParam(required = false) Boolean unassigned, @RequestParam(required = false) String q) {
        return service.list(level, trainer, unassigned, q);
    }

    // The trainer dropdown (one rider), the bulk bar and Reset to assessor. Answers with the new rows so W3 updates
    // in place.
    @PatchMapping("/trainer")
    ReassignResult reassign(@Valid @RequestBody ReassignRidersRequest req) {
        return reassign.reassign(req);
    }
}
