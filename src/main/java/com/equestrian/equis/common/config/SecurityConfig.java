package com.equestrian.equis.common.config;

import com.equestrian.equis.common.security.AuthUserConverter;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;

// Paths have no /api: the context path is stripped before the matchers run.
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final String PUBLIC_PATHS = "/auth/**";

    @Bean
    public AuthUserConverter authUserConverter(TrainerRepository trainerRepository) {
        return new AuthUserConverter(trainerRepository);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthUserConverter authUserConverter,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) throws Exception {
        // Both pass the exception to ApiExceptionHandler, so 401 and 403 use the usual ProblemDetail shape.
        AuthenticationEntryPoint entryPoint = (request, response, ex) ->
                exceptionResolver.resolveException(request, response, null, ex);
        AccessDeniedHandler deniedHandler = (request, response, ex) ->
                exceptionResolver.resolveException(request, response, null, ex);

        http
                // Tokens come from a header, never from cookies.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers("/admin/**").hasRole(Role.ADMIN.name())
                        .requestMatchers("/me/**", "/assessment/**").hasRole(Role.TRAINER.name())
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authUserConverter))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler));
        return http.build();
    }

    // The app calls refresh exactly when its access token has expired. If the old token were read here,
    // the filter would answer 401 before permitAll is reached.
    private static BearerTokenResolver bearerTokenResolver() {
        RequestMatcher publicPaths = PathPatternRequestMatcher.withDefaults().matcher(PUBLIC_PATHS);
        BearerTokenResolver delegate = new DefaultBearerTokenResolver();
        return request -> publicPaths.matches(request) ? null : delegate.resolve(request);
    }
}
