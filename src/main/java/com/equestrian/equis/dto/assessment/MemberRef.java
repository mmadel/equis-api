package com.equestrian.equis.dto.assessment;

import java.util.UUID;

// email is stored lowercase.
public record MemberRef(UUID id, String name, String phone, String email) {
}
