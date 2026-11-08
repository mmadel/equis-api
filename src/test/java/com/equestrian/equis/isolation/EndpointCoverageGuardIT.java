package com.equestrian.equis.isolation;

import com.equestrian.equis.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

// E17 · case 8: every club endpoint has an isolation case. A new endpoint under /me, /admin, /rider or /assessment
// (or /club) fails here, by method and path, until EndpointIsolationIT covers it. /auth and /platform are not club
// endpoints.
class EndpointCoverageGuardIT extends IntegrationTestBase {
    private static final List<String> CLUB_PREFIXES = List.of("/me", "/admin", "/rider", "/assessment", "/club");

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    @Test
    void everyClubEndpointHasAnIsolationCase() {
        Set<String> endpoints = clubEndpoints();

        assertThat(endpoints).as("club endpoints found").isNotEmpty();
        assertThat(endpoints).as("club endpoints without a case in EndpointIsolationIT")
                .isSubsetOf(EndpointIsolationIT.COVERED);
    }

    // And no case points at an endpoint that no longer exists.
    @Test
    void everyIsolationCaseIsARealEndpoint() {
        assertThat(EndpointIsolationIT.COVERED).isSubsetOf(clubEndpoints());
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
