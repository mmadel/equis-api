package com.equestrian.equis.common.tenant;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.service.platform.AccessState;
import com.equestrian.equis.service.platform.ClubAccess;
import com.equestrian.equis.service.platform.ClubAccessService;
import com.equestrian.equis.service.platform.ClubBlockedException;
import com.equestrian.equis.service.platform.SubscriptionExpiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// Runs right after TenantFilter on every request signed in to a club (a token with a cid). The club's state is
// worked out again on each request (one query, no cache), so a renewal or reactivation counts at once.
//   ACTIVE    -> continue
//   READ_ONLY -> reads continue; a request that changes data is refused with subscription_expired
//   BLOCKED   -> everything is refused with club_blocked and the reason
// The super admin (no club), /platform/** and the public /auth/** never get here: they carry no club token.
// Not a @Component, like TenantFilter: SecurityConfig adds it to the security chain.
public class ClubAccessFilter extends OncePerRequestFilter {
    private static final Set<String> READ_METHODS = Set.of(
            HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.OPTIONS.name());
    private static final RequestMatcher PLATFORM = PathPatternRequestMatcher.withDefaults().matcher("/platform/**");
    // Writes a read-only club may still make. Refresh is under /auth (public, so no token is read there); there is
    // no logout endpoint yet: signing out is the app dropping its tokens. Marking a notice read keeps Home usable.
    private static final List<RequestMatcher> READ_ONLY_ALLOWED = List.of(
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/auth/**"),
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/me/notices/{id}/read"));

    private final ClubAccessService clubAccess;
    private final HandlerExceptionResolver exceptionResolver;

    public ClubAccessFilter(ClubAccessService clubAccess, HandlerExceptionResolver exceptionResolver) {
        this.clubAccess = clubAccess;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        UUID club = clubOf(SecurityContextHolder.getContext().getAuthentication());
        if (club == null || PLATFORM.matches(request)) {
            chain.doFilter(request, response);
            return;
        }
        ClubAccess access = clubAccess.forClub(club);
        if (access.state() == AccessState.BLOCKED) {
            // Same ProblemDetail shape as any other error (ApiExceptionHandler).
            exceptionResolver.resolveException(request, response, null, new ClubBlockedException(access.reason()));
            return;
        }
        if (access.state() == AccessState.READ_ONLY && !isAllowedWhileReadOnly(request)) {
            exceptionResolver.resolveException(request, response, null,
                    new SubscriptionExpiredException(access.endsAt()));
            return;
        }
        chain.doFilter(request, response);
    }

    static boolean isAllowedWhileReadOnly(HttpServletRequest request) {
        return READ_METHODS.contains(request.getMethod())
                || READ_ONLY_ALLOWED.stream().anyMatch(matcher -> matcher.matches(request));
    }

    private static UUID clubOf(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser user) {
            return user.clubId();
        }
        return null;
    }
}
