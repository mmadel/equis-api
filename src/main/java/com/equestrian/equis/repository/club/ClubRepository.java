package com.equestrian.equis.repository.club;

import com.equestrian.equis.entity.club.Club;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubRepository extends JpaRepository<Club, UUID> {
    Optional<Club> findByCode(String code);

    boolean existsByCode(String code);

    // The club, its current subscription (SubscriptionRepository.findCurrent: latest started, non-voided) and that
    // subscription's plan, in one query: ClubAccessFilter runs it on every club request. Each row is
    // [Club, Subscription or null, Plan or null]; empty when the club doesn't exist.
    @Query("""
            select c, s, p from Club c
            left join Subscription s on s.clubId = c.id and s.voidedAt is null and s.startsAt <= :now
                and not exists (select 1 from Subscription later where later.clubId = c.id
                                and later.voidedAt is null and later.startsAt <= :now and later.startsAt > s.startsAt)
            left join s.plan p
            where c.id = :id""")
    List<Object[]> findWithCurrentSubscription(@Param("id") UUID id, @Param("now") Instant now);

    // Row lock so plan-limit and subscription checks on one club run one at a time.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Club c where c.id = :id")
    Optional<Club> findByIdForUpdate(@Param("id") UUID id);
}
