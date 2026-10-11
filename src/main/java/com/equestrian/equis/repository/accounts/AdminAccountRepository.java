package com.equestrian.equis.repository.accounts;

import com.equestrian.equis.entity.accounts.AdminAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminAccountRepository extends JpaRepository<AdminAccount, UUID> {
    Optional<AdminAccount> findByEmail(String email);
}
