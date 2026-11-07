package com.equestrian.equis.entity.accounts;

// INVITED: a set-password link was sent, no password yet, can't sign in. ACTIVE: can sign in.
// Stored as written (V27), unlike trainer and rider statuses.
public enum AdminStatus {
    INVITED,
    ACTIVE
}
