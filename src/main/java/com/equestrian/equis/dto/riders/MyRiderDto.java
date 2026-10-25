package com.equestrian.equis.dto.riders;

import com.equestrian.equis.dto.payments.BalanceDto;

import java.time.LocalDate;
import java.util.UUID;

// One row of My riders (screen 4). since is the day the rider was created, in school time;
// nextSession is the earliest upcoming seat, or null. No phone: the list does not show it.
// balance is today's, the same the admin sees on W3; always present.
public record MyRiderDto(UUID id, String name, String initials, int levelId, LocalDate since,
                         UpcomingSeat nextSession, BalanceDto balance) {
}
