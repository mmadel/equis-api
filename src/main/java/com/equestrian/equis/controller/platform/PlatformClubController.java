package com.equestrian.equis.controller.platform;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.dto.platform.ChangeClubStatusRequest;
import com.equestrian.equis.dto.platform.ClubDetail;
import com.equestrian.equis.dto.platform.ClubPage;
import com.equestrian.equis.dto.platform.CreateClubRequest;
import com.equestrian.equis.service.platform.AccessState;
import com.equestrian.equis.service.platform.ClubAdminService;
import com.equestrian.equis.service.platform.ClubFilter;
import com.equestrian.equis.service.platform.ClubOnboardingService;
import com.equestrian.equis.service.platform.ClubSort;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// P1 / P2, the super admin's clubs. SecurityConfig limits /platform/** to the super admin; requests run in root mode.
@RestController
@RequestMapping("/platform/clubs")
public class PlatformClubController {
    static final int DEFAULT_PAGE_SIZE = 25;
    static final int MAX_PAGE_SIZE = 100;

    private final ClubAdminService clubs;
    private final ClubOnboardingService onboarding;

    public PlatformClubController(ClubAdminService clubs, ClubOnboardingService onboarding) {
        this.clubs = clubs;
        this.onboarding = onboarding;
    }

    // sort: name (default), code or endsAt; dir: asc (default) or desc.
    @GetMapping
    ClubPage list(@RequestParam(required = false) String q,
                  @RequestParam(required = false) AccessState state,
                  @RequestParam(required = false) Integer expiringWithinDays,
                  @RequestParam(defaultValue = "name") String sort,
                  @RequestParam(defaultValue = "asc") String dir,
                  @RequestParam(defaultValue = "0") int page,
                  @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        List<ApiFieldError> errors = new ArrayList<>();
        Optional<ClubSort> by = ClubSort.fromParam(sort);
        if (by.isEmpty()) {
            errors.add(new ApiFieldError("sort", "must be name, code or endsAt"));
        }
        if (!dir.equals("asc") && !dir.equals("desc")) {
            errors.add(new ApiFieldError("dir", "must be asc or desc"));
        }
        if (expiringWithinDays != null && expiringWithinDays < 0) {
            errors.add(new ApiFieldError("expiringWithinDays", "must be 0 or more"));
        }
        if (page < 0) {
            errors.add(new ApiFieldError("page", "must be 0 or more"));
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            errors.add(new ApiFieldError("size", "must be between 1 and " + MAX_PAGE_SIZE));
        }
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED,
                    "Some fields are not valid.", Map.of("fieldErrors", List.copyOf(errors)));
        }
        return clubs.list(new ClubFilter(q, state, expiringWithinDays), by.orElseThrow(), dir.equals("desc"),
                page, size);
    }

    // 201 with the new club and its Location.
    @PostMapping
    ResponseEntity<ClubDetail> create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateClubRequest req) {
        ClubDetail club = onboarding.create(req, me.id());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(club.id())
                .toUri();
        return ResponseEntity.created(location).body(club);
    }

    @GetMapping("/{id}")
    ClubDetail get(@PathVariable UUID id) {
        return clubs.get(id);
    }

    @PutMapping("/{id}/status")
    ClubDetail changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeClubStatusRequest req) {
        return clubs.changeStatus(id, req);
    }
}
