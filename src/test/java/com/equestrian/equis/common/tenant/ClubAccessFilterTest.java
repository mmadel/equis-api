package com.equestrian.equis.common.tenant;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

// E17 · club access state, case 4: what a read-only club may still send. Reads always; of the writes only
// refresh (and anything else under /auth) and marking a notice read. There is no logout endpoint.
class ClubAccessFilterTest {

    @ParameterizedTest(name = "{0} {1} -> {2}")
    @CsvSource({
            "GET,     /me/riders,                     true",
            "HEAD,    /admin/riders,                  true",
            "OPTIONS, /me/sessions/1/finish,          true",
            "POST,    /auth/refresh,                  true",
            "POST,    /me/notices/42/read,            true",
            "POST,    /me/sessions/1/finish,          false",
            "POST,    /admin/riders/1/payments,       false",
            "PUT,     /me/sessions/1/assignments,     false",
            "PATCH,   /admin/members/1,               false",
            "DELETE,  /admin/session-templates/1,     false",
            "GET,     /me/notices/42/read,            true",
            "PUT,     /me/notices/42/read,            false",
    })
    void whatAReadOnlyClubMaySend(String method, String path, boolean allowed) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);

        assertThat(ClubAccessFilter.isAllowedWhileReadOnly(request)).isEqualTo(allowed);
    }
}
