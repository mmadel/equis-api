package com.equestrian.equis.repository.platform;

import com.equestrian.equis.entity.platform.SuperAdmin;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SuperAdminRepository extends JpaRepository<SuperAdmin, UUID> {
    Optional<SuperAdmin> findByEmail(String email);

    // Row lock for sign-in, so two wrong guesses at the same moment cannot both read the same counter.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SuperAdmin s where s.email = :email")
    Optional<SuperAdmin> findByEmailForUpdate(@Param("email") String email);
}
