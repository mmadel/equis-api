package com.equestrian.equis.dto.accounts;

// The answer to setting a password through a link: the apps prefill the club code on their sign-in page.
public record InviteAccepted(String clubCode) {
}
