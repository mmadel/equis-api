package com.equestrian.equis.repository.assessment;

import com.equestrian.equis.entity.assessment.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    // Lets the service answer phone_taken before the unique key fires.
    boolean existsByPhone(String phone);
}
