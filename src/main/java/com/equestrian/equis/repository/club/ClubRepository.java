package com.equestrian.equis.repository.club;

import com.equestrian.equis.entity.club.Club;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ClubRepository extends JpaRepository<Club, UUID> {
    Optional<Club> findByCode(String code);

    boolean existsByCode(String code);

    // Row lock so plan-limit and subscription checks on one club run one at a time.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Club c where c.id = :id")
    Optional<Club> findByIdForUpdate(@Param("id") UUID id);
}
