package com.equestrian.equis.dto.riders;

import java.util.List;

public record AdminRiderList(RiderTotals totals, List<TrainerCard> trainers, List<AdminRiderRow> riders) {
}
