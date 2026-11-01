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

    // The open (unused) link of every trainer still invited, with the trainer, oldest link first.
    @Query("""
            select i from Invite i
            join fetch i.trainer t
            where t.status = com.equestrian.equis.entity.accounts.TrainerStatus.INVITED
              and i.acceptedAt is null
            order by i.createdAt, i.id
            """)
    List<Invite> findOpenForInvitedTrainers();

    // Bulk delete so the row is gone before the replacement invite is inserted;
    // a derived delete is flushed after inserts and would trip uq_invite_open_per_trainer.
    @Modifying
    @Transactional
    @Query("delete from Invite i where i.trainer.id = :trainerId and i.acceptedAt is null")
    int deleteByTrainerIdAndAcceptedAtIsNull(@Param("trainerId") UUID trainerId);

    // Same as for trainers: a bulk delete, so the open link is gone before the replacement is inserted
    // (uq_invite_open_per_rider).
    @Modifying
    @Transactional
    @Query("delete from Invite i where i.rider.id = :riderId and i.acceptedAt is null")
    int deleteByRiderIdAndAcceptedAtIsNull(@Param("riderId") UUID riderId);

    // Every link of the trainer, used or not: cancelling an invite removes the trainer.
    @Modifying
    @Transactional
    @Query("delete from Invite i where i.trainer.id = :trainerId")
    int deleteByTrainerId(@Param("trainerId") UUID trainerId);
}
