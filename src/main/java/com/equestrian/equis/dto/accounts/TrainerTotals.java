package com.equestrian.equis.dto.accounts;

// The numbers at the top of W4. invited counts expired invites too; ridersCovered counts only active trainers' riders.
public record TrainerTotals(long active, long invited, long disabled, long ridersCovered) {
}
