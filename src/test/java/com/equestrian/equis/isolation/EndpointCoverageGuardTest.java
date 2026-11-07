package com.equestrian.equis.isolation;

import com.equestrian.equis.support.TestPostgres;
import com.equestrian.equis.support.TrainerAccountsFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

// E17 · case 8: every club endpoint has an isolation case. A new endpoint under /me, /admin, /rider or /assessment
// fails here, by method and path, until EndpointIsolationTest covers it. /auth and /platform are not club endpoints.
@SpringBootTest(properties = "app.sessions.generate-cron=-")
class EndpointCoverageGuardTest {
    private static final List<String> CLUB_PREFIXES = List.of("/me", "/admin", "/rider", "/assessment");

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
        TrainerAccountsFixture.adminAccount(registry);
    }

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    @Test
    void everyClubEndpointHasAnIsolationCase() {
        Set<String> endpoints = clubEndpoints();

        assertThat(endpoints).as("club endpoints found").isNotEmpty();
        assertThat(endpoints).as("club endpoints without a case in EndpointIsolationTest")
                .isSubsetOf(EndpointIsolationTest.COVERED);
    }

    // And no case points at an endpoint that no longer exists.
    @Test
    void everyIsolationCaseIsARealEndpoint() {
        assertThat(EndpointIsolationTest.COVERED).isSubsetOf(clubEndpoints());
    }

    private Set<String> clubEndpoints() {
        Set<String> endpoints = new TreeSet<>();
        for (RequestMappingInfo info : mappings.getHandlerMethods().keySet()) {
            for (String path : info.getPatternValues()) {
                if (CLUB_PREFIXES.stream().noneMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"))) {
                    continue;
                }
                info.getMethodsCondition().getMethods()
                        .forEach(method -> endpoints.add(method.name() + " " + path));
            }
        }
        return endpoints;
    }
}
