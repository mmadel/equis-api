package com.equestrian.equis.common.tenant;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

// Runs right after the JWT filter: the signed-in user's club becomes the request's club.
// A super admin has no club: platform requests run in root mode (every club's rows; writes need runAs(club)).
// Afterwards the previous value is put back, even when the request throws. On a server thread that value is
// always "no club", so the next request on the thread starts clean; tests that set a club keep theirs.
// Not a @Component: SecurityConfig adds it to the security chain, and a bean would also run as a plain filter.
public class TenantFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        UUID previous = TenantContext.raw();
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (isSuperAdmin(authentication)) {
                TenantContext.setRoot();
            } else {
                UUID club = clubOf(authentication);
                if (club != null) {
                    TenantContext.set(club);
                }
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.restore(previous);
        }
    }

    private static boolean isSuperAdmin(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof AuthUser user
                && user.role() == Role.SUPER_ADMIN;
    }

    private static UUID clubOf(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser user) {
            return user.clubId();
        }
        return null;
    }
}
