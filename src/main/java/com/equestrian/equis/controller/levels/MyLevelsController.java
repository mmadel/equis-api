package com.equestrian.equis.controller.levels;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.dto.levels.LevelSessionsResponse;
import com.equestrian.equis.dto.levels.MyLevelSummary;
import com.equestrian.equis.service.levels.MyLevelsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// Everything under /me is for the trainer role; see SecurityConfig.
@RestController
@RequestMapping("/me/levels")
public class MyLevelsController {
    private final MyLevelsService service;

    public MyLevelsController(MyLevelsService service) {
        this.service = service;
    }

    @GetMapping
    List<MyLevelSummary> list(@AuthenticationPrincipal AuthUser user) {
        return service.summaries(user.id());
    }

    @GetMapping("/{id}/sessions")
    LevelSessionsResponse sessions(@AuthenticationPrincipal AuthUser user, @PathVariable short id,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.sessions(user.id(), id, from, to);
    }
}
