package com.equestrian.equis.controller.riders;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.dto.riders.LevelChangeRequest;
import com.equestrian.equis.dto.riders.LevelChanged;
import com.equestrian.equis.dto.riders.MyRiderDto;
import com.equestrian.equis.dto.riders.RiderLevelDto;
import com.equestrian.equis.service.riders.LevelChangeService;
import com.equestrian.equis.service.riders.MyRidersQueryService;
import com.equestrian.equis.service.riders.RiderLevelQueryService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// Everything under /me is for the trainer role; see SecurityConfig.
@RestController
@RequestMapping("/me/riders")
public class MyRidersController {
    private final MyRidersQueryService query;
    private final RiderLevelQueryService riderLevels;
    private final LevelChangeService levelChanges;

    public MyRidersController(MyRidersQueryService query, RiderLevelQueryService riderLevels,
                              LevelChangeService levelChanges) {
        this.query = query;
        this.riderLevels = riderLevels;
        this.levelChanges = levelChanges;
    }

    // The whole list in one answer; tabs, counts and search are done in the app.
    @GetMapping
    List<MyRiderDto> list(@AuthenticationPrincipal AuthUser user) {
        return query.myRiders(user.id());
    }

    // The whole Rider level page (4a) and its confirm sheet's impact (4b) in one answer.
    @GetMapping("/{id}/level-history")
    RiderLevelDto levelHistory(@PathVariable UUID id, @AuthenticationPrincipal AuthUser user) {
        return riderLevels.riderLevel(id, user.id());
    }

    // Promote or demote from the confirm sheet (4b). Bean Validation checks the shape; the service runs every rule.
    @PostMapping("/{id}/level-change")
    LevelChanged changeLevel(@PathVariable UUID id, @AuthenticationPrincipal AuthUser user,
                             @Valid @RequestBody LevelChangeRequest req) {
        return levelChanges.change(id, user.id(), req);
    }
}
