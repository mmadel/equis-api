package com.equestrian.equis.service.assessment;

import com.equestrian.equis.entity.assessment.MemberStatus;

// Works out a person's status; shared by the W2 list, the close rules and the tests.
public final class MemberStatusResolver {
    private MemberStatusResolver() {
    }

    // latestDisplayStatus is AssessmentBooking.displayStatus(now, zone) of the person's latest booking, or null.
    // A placed booking always has a rider, so it counts as rider even before the rider is looked up.
    public static MemberStatus resolve(boolean closed, boolean rider, String latestDisplayStatus) {
        if (closed) {
            return MemberStatus.CLOSED;
        }
        if (rider || "placed".equals(latestDisplayStatus)) {
            return MemberStatus.RIDER;
        }
        if ("noshow".equals(latestDisplayStatus)) {
            return MemberStatus.NO_SHOW;
        }
        return MemberStatus.PENDING_ASSESSMENT; // upcoming, waiting, cancelled, or no booking
    }
}
