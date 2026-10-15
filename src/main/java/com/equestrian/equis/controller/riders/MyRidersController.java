package com.equestrian.equis.controller.riders;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.dto.riders.MyRiderDto;
import com.equestrian.equis.service.riders.MyRidersQueryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Everything under /me is for the trainer role; see SecurityConfig.
@RestController
@RequestMapping("/me/riders")
public class MyRidersController {
    private final MyRidersQueryService query;

    public MyRidersController(MyRidersQueryService query) {
        this.query = query;
    }

    // The whole list in one answer; tabs, counts and search are done in the app.
    @GetMapping
    List<MyRiderDto> list(@AuthenticationPrincipal AuthUser user) {
        return query.myRiders(user.id());
    }
}
