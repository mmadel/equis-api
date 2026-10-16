package com.equestrian.equis.repository.riders;

import java.util.UUID;

// The rider a member became when placed.
public record MemberRider(UUID memberId, UUID riderId) {
}
