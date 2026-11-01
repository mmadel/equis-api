package com.equestrian.equis.dto.riderapp;

import com.equestrian.equis.dto.riders.LevelHistoryEntry;

import java.util.List;

// starTrend: the last 12 months with evaluations, oldest first. levelHistory: newest first, placement last.
public record RiderProgress(List<StarTrendPoint> starTrend, List<LevelHistoryEntry> levelHistory) {
}
