package com.equestrian.equis.controller.platform;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.platform.AccountFilter;
import com.equestrian.equis.dto.platform.AccountPage;
import com.equestrian.equis.dto.platform.AccountType;
import com.equestrian.equis.dto.platform.PlatformAccountStatus;
import com.equestrian.equis.service.platform.PlatformAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// The Accounts tab of a club on the platform console. SecurityConfig limits /platform/** to the super admin; requests
// run in root mode. An unknown type or status value is a 400 validation_failed (ApiExceptionHandler.handleTypeMismatch).
@RestController
@RequestMapping("/platform/clubs/{clubId}/accounts")
public class PlatformAccountController {
    static final int DEFAULT_PAGE_SIZE = 25;
    static final int MAX_PAGE_SIZE = 100;

    private final PlatformAccountService accounts;

    public PlatformAccountController(PlatformAccountService accounts) {
        this.accounts = accounts;
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
}
