package com.equestrian.equis.service.platform;

import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.entity.platform.Plan;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Trainers and riders each club uses, for the platform screens. One grouped query per table for all the clubs asked.
//   trainers: not disabled (invited and active)
//   riders:   people (member) not closed: waiting for assessment, no-show and riders
// The same counts the plan limits use later.
@Service
public class ClubUsageService {
    private static final String TRAINERS = """
            SELECT club_id, count(*) AS n FROM trainer
            WHERE club_id IN (:clubIds) AND status <> 'disabled'
            GROUP BY club_id""";
    private static final String RIDERS = """
            SELECT club_id, count(*) AS n FROM member
            WHERE club_id IN (:clubIds) AND closed_at IS NULL
            GROUP BY club_id""";

    private final NamedParameterJdbcTemplate jdbc;

    public ClubUsageService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // plans gives each club's current plan, for the limits; a club missing from it shows no limits.
    // Reads across clubs, so it runs in root mode only (in a club, row-level security would hide the others).
    public Map<UUID, Usage> usageFor(Collection<UUID> clubIds, Map<UUID, Plan> plans) {
        if (!TenantContext.isRoot()) {
            throw new IllegalStateException("Club usage is read in root mode");
        }
        if (clubIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Integer> trainers = counts(TRAINERS, clubIds);
        Map<UUID, Integer> riders = counts(RIDERS, clubIds);
        Map<UUID, Usage> usage = new HashMap<>();
        for (UUID club : clubIds) {
            Plan plan = plans.get(club);
            usage.put(club, new Usage(trainers.getOrDefault(club, 0), plan == null ? null : plan.getMaxTrainers(),
                    riders.getOrDefault(club, 0), plan == null ? null : plan.getMaxRiders()));
        }
        return usage;
    }

    private Map<UUID, Integer> counts(String sql, Collection<UUID> clubIds) {
        Map<UUID, Integer> counts = new HashMap<>();
        jdbc.query(sql, Map.of("clubIds", clubIds),
                rs -> {
                    counts.put(rs.getObject("club_id", UUID.class), rs.getInt("n"));
                });
        return counts;
    }
}
