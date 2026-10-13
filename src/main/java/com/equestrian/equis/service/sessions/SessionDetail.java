package com.equestrian.equis.service.sessions;

import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.entity.sessions.Session;

import java.util.List;

// One level session as a trainer sees it: seats are every trainer's assignments,
// myRiders are the trainer's riders in the session's level, ordered by name.
public record SessionDetail(Session session, List<Assignment> seats, List<Rider> myRiders, boolean readOnly) {
}
