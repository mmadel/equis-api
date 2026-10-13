package com.equestrian.equis.common.error;

public class ErrorCodes {
    private ErrorCodes() {}

    public static final String VALIDATION_FAILED  = "validation_failed";  // 400
    public static final String INVALID_LINK       = "invalid_link";       // 400
    public static final String UNAUTHORIZED          = "unauthorized";          // 401
    public static final String INVALID_REFRESH_TOKEN = "invalid_refresh_token"; // 401
    public static final String INVALID_CREDENTIALS   = "invalid_credentials";   // 401
    public static final String FORBIDDEN             = "forbidden";             // 403
    public static final String ACCOUNT_DISABLED      = "account_disabled";      // 403
    public static final String NOT_YOUR_RIDER     = "not_your_rider";     // 403
    public static final String NOT_FOUND          = "not_found";          // 404
    public static final String SEAT_TAKEN         = "seat_taken";         // 409
    public static final String CAPACITY_EXCEEDED  = "capacity_exceeded";  // 409
    public static final String ALREADY_BOOKED     = "already_booked";     // 409
    public static final String PHONE_TAKEN        = "phone_taken";        // 409
    public static final String SESSION_CLOSED     = "session_closed";     // 409
    public static final String LEVEL_OUT_OF_RANGE = "level_out_of_range"; // 422
    public static final String RESULT_TOO_EARLY   = "result_too_early";   // 422
    public static final String ACCOUNT_LOCKED     = "account_locked";     // 423
    public static final String INTERNAL_ERROR     = "internal_error";     // 500
}
