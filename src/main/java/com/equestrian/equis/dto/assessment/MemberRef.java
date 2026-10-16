package com.equestrian.equis.dto.assessment;

import java.util.UUID;

// phone is null when the member gave none.
public record MemberRef(UUID id, String name, String phone) {
}
