package com.equestrian.equis.dto.platform;

// The Accounts tab's filters; null means "any".
//   q       part of the name, email or phone, any case (already trimmed, at least 2 characters)
//   type    one account type
//   status  one mapped status
public record AccountFilter(String q, AccountType type, PlatformAccountStatus status) {
}
