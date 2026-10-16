package com.equestrian.equis.repository.assessment;

import java.util.UUID;

// How many results one assessor recorded in a time window.
public interface AssessorCount {
    UUID getAssessorId();

    long getRecorded();
}
