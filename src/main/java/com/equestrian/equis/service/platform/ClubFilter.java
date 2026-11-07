package com.equestrian.equis.service.platform;

// P1's filters; null means "any".
//   q                   part of the club's name or code, any case
//   state               the access state now
//   expiringWithinDays  the current subscription still runs and ends within this many days
public record ClubFilter(String q, AccessState state, Integer expiringWithinDays) {
}
