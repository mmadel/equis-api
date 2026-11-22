package com.equestrian.equis.dto.platform;

// One status for every account type on the platform list:
//   admin   INVITED / ACTIVE                       -> same
//   trainer invited / active / disabled            -> INVITED / ACTIVE / DISABLED
//   rider   none / invited / active                -> NO_ACCOUNT / INVITED / ACTIVE
public enum PlatformAccountStatus {
    ACTIVE,
    INVITED,
    DISABLED,
    NO_ACCOUNT
}
