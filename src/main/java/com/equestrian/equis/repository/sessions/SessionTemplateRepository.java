package com.equestrian.equis.repository.sessions;

import com.equestrian.equis.entity.sessions.SessionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionTemplateRepository extends JpaRepository<SessionTemplate, UUID> {
    List<SessionTemplate> findByLevelIdAndDeletedAtIsNullOrderByDayOfWeekAscStartTimeAsc(Short levelId);

    List<SessionTemplate> findByLevelIsNullAndDeletedAtIsNullOrderByDayOfWeekAscStartTimeAsc();

    List<SessionTemplate> findByDeletedAtIsNull();

    Optional<SessionTemplate> findByIdAndDeletedAtIsNull(UUID id);
}
