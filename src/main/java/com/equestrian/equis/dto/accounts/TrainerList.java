package com.equestrian.equis.dto.accounts;

import java.util.List;

public record TrainerList(TrainerTotals totals, List<TrainerRow> trainers) {
}
