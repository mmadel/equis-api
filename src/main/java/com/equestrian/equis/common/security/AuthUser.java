package com.equestrian.equis.common.security;

import java.util.UUID;

public record AuthUser(UUID id, Role role) {}
