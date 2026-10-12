package com.equestrian.equis.repository.accounts;

import com.equestrian.equis.entity.accounts.AdminAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AdminAccountRepository extends JpaRepository<AdminAccount, UUID> {
    Optional<AdminAccount> findByEmail(String email);

    // Row lock for sign-in, so two wrong guesses at the same moment cannot both read the same counter.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AdminAccount a where a.email = :email")
    Optional<AdminAccount> findByEmailForUpdate(@Param("email") String email);
}
