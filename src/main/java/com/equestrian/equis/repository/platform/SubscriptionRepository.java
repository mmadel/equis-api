package com.equestrian.equis.repository.platform;

import com.equestrian.equis.entity.platform.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    // The club's current subscription: its non-voided row with the latest start that has begun by now.
    // It may have ended already; check isValidAt(now). Empty when no row has started.
    default Optional<Subscription> findCurrent(UUID clubId, Instant now) {
        return findFirstByClubIdAndVoidedAtIsNullAndStartsAtLessThanEqualOrderByStartsAtDesc(clubId, now);
    }

    Optional<Subscription> findFirstByClubIdAndVoidedAtIsNullAndStartsAtLessThanEqualOrderByStartsAtDesc(
            UUID clubId, Instant now);

    // The last end of the club's non-voided rows, where a renewal starts. Empty when it has none.
    @Query("select max(s.endsAt) from Subscription s where s.clubId = :clubId and s.voidedAt is null")
    Optional<Instant> findLatestEnd(@Param("clubId") UUID clubId);

    // Only to undo a club whose onboarding failed half way (ClubOnboardingService); rows are never deleted otherwise.
    @Modifying
    @Query("delete from Subscription s where s.clubId = :clubId")
    int deleteByClubId(@Param("clubId") UUID clubId);

    // The whole history, voided rows included, newest start first.
    List<Subscription> findAllByClubIdOrderByStartsAtDesc(UUID clubId);

    // findCurrent for many clubs in one query (club lists). Clubs without a started row are left out.
    default List<Subscription> findCurrentForClubs(Collection<UUID> clubIds, Instant now) {
        return clubIds.isEmpty() ? List.of() : findCurrentForClubIds(clubIds, now);
    }

    @Query(value = """
            SELECT DISTINCT ON (club_id) *
            FROM subscription
            WHERE club_id IN (:clubIds) AND voided_at IS NULL AND starts_at <= :now
            ORDER BY club_id, starts_at DESC
            """, nativeQuery = true)
    List<Subscription> findCurrentForClubIds(@Param("clubIds") Collection<UUID> clubIds, @Param("now") Instant now);
}
