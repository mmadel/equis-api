package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

// Release 2: the admin records new people in the web console, so the trainer's walk-in (A1) is gone.
// The body is not read, so any request answers 410. This class is deleted in a later E11 task.
@RestController
public class MemberController {

    @PostMapping("/me/members")
    void register() {
        throw new ApiException(HttpStatus.GONE, ErrorCodes.ENDPOINT_REMOVED,
                "Trainers no longer register new people; the admin does this in the web console.");
    }
}
