package com.equestrian.equis.service.sessions;

// What deleting a template did: how many upcoming sessions were cancelled.
public record DeleteImpact(int cancelledSessions) {
}
