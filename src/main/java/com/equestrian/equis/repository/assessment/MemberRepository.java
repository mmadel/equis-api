package com.equestrian.equis.repository.assessment;

import com.equestrian.equis.entity.assessment.Member;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
