package com.equestrian.equis.repository.accounts;

import com.equestrian.equis.entity.accounts.Trainer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TrainerRepository extends JpaRepository<Trainer, UUID> {
    Optional<Trainer> findByEmail(String email);

    Optional<Trainer> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
