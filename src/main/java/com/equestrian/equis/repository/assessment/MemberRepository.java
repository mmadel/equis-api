package com.equestrian.equis.repository.assessment;

import com.equestrian.equis.entity.assessment.Member;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    // Lets the service answer phone_taken before the unique key fires.
    boolean existsByPhone(String phone);

    // Emails are stored lowercase, so pass a lowercase one.
    boolean existsByEmail(String email);

    // Let the intake point the admin at the person who already has this phone or email.
    Optional<Member> findByPhone(String phone);

    Optional<Member> findByEmail(String email);

    // Row lock held until the transaction ends, so closing, reopening and rebooking a person run one after the other.
    // Call it before the member is loaded in this transaction: an entity already loaded comes back as it was.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") UUID id);
}
