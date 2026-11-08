package com.equestrian.equis.controller.club;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.dto.club.ClubStatusView;
import com.equestrian.equis.service.platform.ClubAccessService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// The signed-in person's club and its access state, for the apps' banners. Admins, trainers and riders
// (SecurityConfig). Works while read-only (a GET); a blocked club gets 403 club_blocked from ClubAccessFilter.
@RestController
public class ClubStatusController {
    private final ClubAccessService clubAccess;

    public ClubStatusController(ClubAccessService clubAccess) {
        this.clubAccess = clubAccess;
    }

    @GetMapping("/club/status")
    ClubStatusView status(@AuthenticationPrincipal AuthUser me) {
        return clubAccess.status(me.clubId());
    }
}
