package com.equestrian.equis.common.tenant;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// E17 · case 2: the filter gives the request the signed-in user's club and never leaves it on the thread.
class TenantFilterTest {
    private static final UUID CLUB = UUID.fromString("00000000-0000-0000-0000-00000000000a");

    private final TenantFilter filter = new TenantFilter();

    @BeforeEach
    @AfterEach
    void clean() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void theRequestRunsInTheUsersClub() throws Exception {
        signIn();
        UUID[] seen = new UUID[1];

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                new MockFilterChain(new jakarta.servlet.http.HttpServlet() {
                    @Override
                    protected void service(jakarta.servlet.http.HttpServletRequest req,
                                           jakarta.servlet.http.HttpServletResponse res) {
                        seen[0] = TenantContext.current();
                    }
                }));

        assertThat(seen[0]).isEqualTo(CLUB);
        assertThat(TenantContext.current()).isNull();
    }

    // Case 2
    @Test
    void aRequestThatThrowsStillClearsTheClub() {
        signIn();

        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (request, response) -> {
                    throw new ServletException("boom");
                })).isInstanceOf(ServletException.class);

        assertThat(TenantContext.current()).isNull();
        assertThat(TenantContext.isRoot()).isFalse();
    }

    @Test
    void anAnonymousRequestRunsWithNoClub() throws Exception {
        UUID[] seen = {CLUB};

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (request, response) -> seen[0] = TenantContext.current());

        assertThat(seen[0]).isNull();
    }

    private static void signIn() {
        AuthUser user = new AuthUser(UUID.randomUUID(), Role.TRAINER, CLUB);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
    }
}
