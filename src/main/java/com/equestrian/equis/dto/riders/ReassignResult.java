package com.equestrian.equis.dto.riders;

import java.util.List;

// unchanged counts the riders who already had that trainer. riders has one row per requested rider, as saved.
public record ReassignResult(int updated, int unchanged, List<AdminRiderRow> riders) {
}
