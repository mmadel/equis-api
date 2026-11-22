package com.equestrian.equis.controller.platform;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.dto.platform.AccountFilter;
import com.equestrian.equis.dto.platform.AccountPage;
import com.equestrian.equis.dto.platform.AccountType;
import com.equestrian.equis.dto.platform.PlatformAccountReset;
import com.equestrian.equis.dto.platform.PlatformAccountStatus;
import com.equestrian.equis.service.platform.PlatformAccountActionService;
import com.equestrian.equis.service.platform.PlatformAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// The Accounts tab of a club on the platform console. SecurityConfig limits /platform/** to the super admin; requests
// run in root mode. An unknown type or status value is a 400 validation_failed (ApiExceptionHandler.handleTypeMismatch).
// The account actions take the type in the path, lowercase (admin, trainer, rider), because the three accounts live
// in three tables.
@RestController
@RequestMapping("/platform/clubs/{clubId}/accounts")
public class PlatformAccountController {
    static final int DEFAULT_PAGE_SIZE = 25;
    static final int MAX_PAGE_SIZE = 100;

    private final PlatformAccountService accounts;
    private final PlatformAccountActionService actions;

    public PlatformAccountController(PlatformAccountService accounts, PlatformAccountActionService actions) {
        this.accounts = accounts;
        this.actions = actions;
    }

    @GetMapping
    AccountPage list(@PathVariable UUID clubId,
                     @RequestParam(required = false) String q,
                     @RequestParam(required = false) AccountType type,
                     @RequestParam(required = false) PlatformAccountStatus status,
                     @RequestParam(defaultValue = "0") int page,
                     @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        List<ApiFieldError> errors = new ArrayList<>();
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
        return accounts.list(clubId, new AccountFilter(q, type, status), page, size);
    }

    // A 24-hour set-password link for an active account, emailed after the save. No body.
    @PostMapping("/{type}/{id}/reset-password")
    PlatformAccountReset resetPassword(@AuthenticationPrincipal AuthUser me, @PathVariable UUID clubId,
                                       @PathVariable String type, @PathVariable UUID id) {
        return actions.resetPassword(clubId, pathType(type), id, me.id());
    }

    // Only the lowercase names; anything else is answered like a bad enum value elsewhere.
    static AccountType pathType(String value) {
        return switch (value) {
            case "admin" -> AccountType.ADMIN;
            case "trainer" -> AccountType.TRAINER;
            case "rider" -> AccountType.RIDER;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED,
                    "Some fields are not valid.",
                    Map.of("fieldErrors", List.of(new ApiFieldError("type", ErrorCodes.INVALID_VALUE))));
        };
    }
}
