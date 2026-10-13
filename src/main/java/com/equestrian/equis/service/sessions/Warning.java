package com.equestrian.equis.service.sessions;

import java.util.List;

// Something the admin should know about but that does not stop the save.
// overlaps is empty for warnings that are not about overlapping templates.
public record Warning(String code, List<OverlapRef> overlaps) {
    public static final String OVERLAP = "overlap";

    public Warning {
        overlaps = List.copyOf(overlaps);
    }

    public static Warning overlap(List<OverlapRef> overlaps) {
        return new Warning(OVERLAP, overlaps);
    }
}
