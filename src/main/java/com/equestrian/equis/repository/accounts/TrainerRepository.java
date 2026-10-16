package com.equestrian.equis.repository.accounts;

import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TrainerRepository extends JpaRepository<Trainer, UUID> {
    Optional<Trainer> findByEmail(String email);

    Optional<Trainer> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByIdAndStatus(UUID id, TrainerStatus status);

    // Row lock held until the transaction ends, so a disable and an enable, or a disable and a reset link,
    // run one after the other.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trainer t where t.id = :id")
    Optional<Trainer> findByIdForUpdate(@Param("id") UUID id);
}
