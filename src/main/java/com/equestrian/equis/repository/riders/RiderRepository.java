package com.equestrian.equis.repository.riders;

import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.assessment.BookingStatus;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.riders.RiderAccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiderRepository extends JpaRepository<Rider, UUID> {
    List<Rider> findByTrainerIdAndLevelIdOrderByName(UUID trainerId, Short levelId);

    // All the trainer's riders for My riders, by level then name ignoring case. Unassigned riders have no trainer.
    @Query("""
            select r from Rider r
            where r.trainer.id = :trainerId
            order by r.level.id, lower(r.name)
            """)
    List<Rider> findMine(@Param("trainerId") UUID trainerId);

    long countByTrainerId(UUID trainerId);

    boolean existsByTrainerId(UUID trainerId);

    // The one definition of "needs reassignment": no trainer, or a disabled one. Computed, never stored,
    // so enabling the trainer clears it. W3 (E7) uses it.
    @Query("""
            select r from Rider r
            left join r.trainer t
            where t is null or t.status = :disabled
            order by r.name
            """)
    List<Rider> findNeedingReassignment(@Param("disabled") TrainerStatus disabled);

    default List<Rider> findNeedingReassignment() {
        return findNeedingReassignment(TrainerStatus.DISABLED);
    }

    // W3: every rider with their trainer and the assessor of their placed booking, in one query. A member has at
    // most one placed booking (a placed member can't be rebooked), so each rider gives one row. Unordered.
    @Query("""
            select new com.equestrian.equis.repository.riders.AdminRiderView(
                r.id, r.name, r.phone, r.level.id, r.createdAt,
                t.id, t.name, t.status, a.id, a.name, a.status)
            from Rider r
            left join r.trainer t
            left join AssessmentBooking b on b.member.id = r.memberId and b.status = :placed
            left join b.assessor a
            """)
    List<AdminRiderView> findAdminView(@Param("placed") BookingStatus placed);

    // The same rows for these riders only. Don't call with an empty list.
    @Query("""
            select new com.equestrian.equis.repository.riders.AdminRiderView(
                r.id, r.name, r.phone, r.level.id, r.createdAt,
                t.id, t.name, t.status, a.id, a.name, a.status)
            from Rider r
            left join r.trainer t
            left join AssessmentBooking b on b.member.id = r.memberId and b.status = :placed
            left join b.assessor a
            where r.id in :ids
            """)
    List<AdminRiderView> findAdminViewByIds(@Param("placed") BookingStatus placed,
                                            @Param("ids") Collection<UUID> ids);

    // Emails are stored lowercase; callers pass a lowercased email.
    Optional<Rider> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByIdAndAccountStatus(UUID id, RiderAccountStatus accountStatus);

    // Whether the member was already placed as a rider.
    boolean existsByMemberId(UUID memberId);

    // The riders these members became; members never placed are absent. Don't call with an empty list.
    @Query("""
            select new com.equestrian.equis.repository.riders.MemberRider(r.memberId, r.id)
            from Rider r
            where r.memberId in :memberIds
            """)
    List<MemberRider> findMemberRiderIds(@Param("memberIds") Collection<UUID> memberIds);

    // One row per level the trainer has riders in; levels without riders are absent.
    @Query("""
            select r.level.id as levelId, count(r) as riderCount
            from Rider r
            where r.trainer.id = :trainerId
            group by r.level.id
            order by r.level.id
            """)
    List<LevelRiderCount> countByTrainerPerLevel(@Param("trainerId") UUID trainerId);

    // One row per trainer and level they have riders in; empty pairs are absent. Don't call with an empty list.
    @Query("""
            select r.trainer.id as trainerId, r.level.id as levelId, count(r) as riderCount
            from Rider r
            where r.trainer.id in :trainerIds
            group by r.trainer.id, r.level.id
            """)
    List<TrainerLevelCount> countByTrainerIdsAndLevel(@Param("trainerIds") Collection<UUID> trainerIds);

    // Row lock held until the transaction ends, so two level changes, or a level change and a seat save, cannot cross.
    // No join on level: after waiting for the lock, PostgreSQL re-checks a join against the old level row and
    // would drop the rider; and Hibernate cannot lock the immutable Level. Callers only need the level's id.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rider r where r.id = :id")
    Optional<Rider> findByIdForUpdate(@Param("id") UUID id);

    // W3 trainer changes: row locks taken in id order, so two overlapping bulk changes cannot deadlock. No join on
    // trainer, for the same reason as findByIdForUpdate.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rider r where r.id in :ids order by r.id")
    List<Rider> findAllByIdInForUpdate(@Param("ids") Collection<UUID> ids);

    // FOR SHARE: a seat save waits for a level change on the same riders, and reads their new level.
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select r from Rider r where r.id in :ids")
    List<Rider> findAllForShareByIdIn(@Param("ids") Collection<UUID> ids);
}
