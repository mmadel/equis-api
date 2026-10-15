package com.equestrian.equis.repository.riders;

import com.equestrian.equis.entity.riders.Rider;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiderRepository extends JpaRepository<Rider, UUID> {
    List<Rider> findByTrainerIdAndLevelIdOrderByName(UUID trainerId, Short levelId);

    // All the trainer's riders for My riders, by level then name ignoring case. Unassigned riders have no trainer.
    @Query("""
            select r from Rider r
            where r.trainer.id = :trainerId
            order by r.level.id, lower(r.name)
            """)
    List<Rider> findMine(@Param("trainerId") UUID trainerId);

    // Whether the member was already placed as a rider.
    boolean existsByMemberId(UUID memberId);

    // One row per level the trainer has riders in; levels without riders are absent.
    @Query("""
            select r.level.id as levelId, count(r) as riderCount
            from Rider r
            where r.trainer.id = :trainerId
            group by r.level.id
            order by r.level.id
            """)
    List<LevelRiderCount> countByTrainerPerLevel(@Param("trainerId") UUID trainerId);

    // Row lock held until the transaction ends, so two level changes, or a level change and a seat save, cannot cross.
    // No join on level: after waiting for the lock, PostgreSQL re-checks a join against the old level row and
    // would drop the rider; and Hibernate cannot lock the immutable Level. Callers only need the level's id.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rider r where r.id = :id")
    Optional<Rider> findByIdForUpdate(@Param("id") UUID id);

    // FOR SHARE: a seat save waits for a level change on the same riders, and reads their new level.
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select r from Rider r where r.id in :ids")
    List<Rider> findAllForShareByIdIn(@Param("ids") Collection<UUID> ids);
}
