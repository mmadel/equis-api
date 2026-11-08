package com.equestrian.equis.support;

import com.equestrian.equis.service.platform.AccessState;
import com.equestrian.equis.service.platform.ClubAccess;
import com.equestrian.equis.service.platform.ClubAccessService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.ZoneId;
import java.util.UUID;

// For @WebMvcTest slices that import SecurityConfig: its ClubAccessFilter needs a ClubAccessService, and a slice
// has no database. Every club is ACTIVE, so the slice tests see the same answers as before E17.
public class ActiveClubAccess extends ClubAccessService {
    private ActiveClubAccess() {
        super(null, null, ZoneId.of("Africa/Cairo"), Clock.systemUTC());
    }

    @Override
    public ClubAccess forClub(UUID clubId) {
        return new ClubAccess(AccessState.ACTIVE, null, null, null, null, 0);
    }

    @TestConfiguration
    public static class Config {
        @Bean
        ClubAccessService clubAccessService() {
            return new ActiveClubAccess();
        }
    }
}
