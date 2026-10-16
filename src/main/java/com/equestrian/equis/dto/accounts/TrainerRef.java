package com.equestrian.equis.dto.accounts;

import java.util.UUID;

// A trainer as other screens show them (W2's assessor, W3's trainer). status is "invited", "active" or "disabled".
public record TrainerRef(UUID id, String name, String status) {
}
