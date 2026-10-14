package com.equestrian.equis.repository.riders;

import com.equestrian.equis.entity.riders.Rider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RiderRepository extends JpaRepository<Rider, UUID> {
    List<Rider> findByTrainerIdAndLevelIdOrderByName(UUID trainerId, Short levelId);

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
}
