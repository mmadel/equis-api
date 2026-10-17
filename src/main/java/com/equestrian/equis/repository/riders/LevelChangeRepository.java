package com.equestrian.equis.repository.riders;

import com.equestrian.equis.entity.riders.LevelChange;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LevelChangeRepository extends JpaRepository<LevelChange, UUID> {
    // The 4a history list, newest first, with the trainer names in the same query.
    @Query("""
            select c from LevelChange c
            join fetch c.byTrainer
            where c.rider.id = :riderId
            order by c.createdAt desc
            """)
    List<LevelChange> findHistory(@Param("riderId") UUID riderId);

    // The latest promotions and demotions of any rider, with rider and trainer, newest first. Every row has a
    // fromLevel (the column is not null): placements live on assessment_booking. The levels are read by id only.
    @Query("""
            select c from LevelChange c
            join fetch c.rider
            join fetch c.byTrainer
            order by c.createdAt desc, c.id
            """)
    List<LevelChange> findRecentChanges(Pageable page);
}
