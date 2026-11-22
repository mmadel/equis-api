package com.equestrian.equis.repository.platform;

import com.equestrian.equis.entity.platform.PlatformAccountAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlatformAccountActionRepository extends JpaRepository<PlatformAccountAction, UUID> {
}
