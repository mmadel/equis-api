package com.equestrian.equis.service.accounts;

// Who sent an account link: the club's admin (from the console, or the club's onboarding) or the equis team from
// the platform. It changes only the first line of a reset email.
public enum LinkSender {
    CLUB_ADMIN,
    PLATFORM
}
