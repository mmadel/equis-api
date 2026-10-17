package com.equestrian.equis.controller.sessions;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.dto.sessions.MyWeek;
import com.equestrian.equis.service.sessions.MyWeekService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

// Everything under /me is for the trainer role; see SecurityConfig.
@RestController
@RequestMapping("/me/week")
public class MyWeekController {
    private final MyWeekService service;

    public MyWeekController(MyWeekService service) {
        this.service = service;
    }

    // date can be any day of the wanted week; without it, the current school week.
    @GetMapping
    MyWeek week(@AuthenticationPrincipal AuthUser user,
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.week(user.id(), date);
    }
}
