package com.equestrian.equis.repository.accounts;

import com.equestrian.equis.entity.accounts.RefreshToken;
import com.equestrian.equis.entity.accounts.SubjectType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // Locks the row until the transaction ends, so two refreshes with the same token cannot both rotate it.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshToken r where r.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    // Bulk update, so RefreshToken entities already loaded in the persistence context keep their old revokedAt.
    @Modifying
    @Transactional
    @Query("update RefreshToken r set r.revokedAt = :now "
            + "where r.subjectType = :type and r.subjectId = :subjectId and r.revokedAt is null")
    int revokeAllActive(@Param("type") SubjectType type, @Param("subjectId") UUID subjectId, @Param("now") Instant now);

    // subject_id has no foreign key, so nothing cascades when a trainer or admin row is deleted.
    @Modifying
    @Transactional
    @Query("delete from RefreshToken r where r.subjectType = :type and r.subjectId = :subjectId")
    int deleteBySubjectTypeAndSubjectId(@Param("type") SubjectType type, @Param("subjectId") UUID subjectId);
}
