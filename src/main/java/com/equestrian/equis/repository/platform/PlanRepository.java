package com.equestrian.equis.repository.platform;

import com.equestrian.equis.entity.platform.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {
    Optional<Plan> findByCode(String code);

    // The trial plan; there is at most one (uq_plan_single_trial).
    Optional<Plan> findByTrialDaysIsNotNull();

    List<Plan> findAllByOrderByCodeAsc();
}
