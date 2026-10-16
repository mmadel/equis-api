package com.equestrian.equis.repository.accounts;

import com.equestrian.equis.entity.accounts.Invite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InviteRepository extends JpaRepository<Invite, UUID> {
    Optional<Invite> findByTokenHash(String tokenHash);

    // The open (unused) link of each trainer, at most one each. Don't call with an empty list.
    List<Invite> findByTrainerIdInAndAcceptedAtIsNull(Collection<UUID> trainerIds);

    // Bulk delete so the row is gone before the replacement invite is inserted;
    // a derived delete is flushed after inserts and would trip uq_invite_open_per_trainer.
    @Modifying
    @Transactional
    @Query("delete from Invite i where i.trainer.id = :trainerId and i.acceptedAt is null")
    int deleteByTrainerIdAndAcceptedAtIsNull(@Param("trainerId") UUID trainerId);
}
