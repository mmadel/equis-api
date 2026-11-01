package com.equestrian.equis.repository.evaluations;

// The average stars of one calendar month, in the column types JPQL gives back.
public record StarTrendRow(Integer year, Integer month, Double averageStars, Long evaluations) {
}
