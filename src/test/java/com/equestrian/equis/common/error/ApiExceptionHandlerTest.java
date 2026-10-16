package com.equestrian.equis.common.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ApiExceptionHandlerTest.TestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandlerTest.TestController.class)
public class ApiExceptionHandlerTest {
    @Autowired
    MockMvc mvc;

    @RestController
    static class TestController {

        @GetMapping("/test/conflict")
        void conflict() {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.SEAT_TAKEN, "That seat is already taken.");
        }

        @GetMapping("/test/extra")
        void extra() {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.INVALID_CREDENTIALS,
                    "Wrong email or password.", Map.of("attemptsLeft", 3));
        }

        @PostMapping("/test/validate")
        void validate(@Valid @RequestBody Body body) {}

        @GetMapping("/test/boom")
        void boom() {
            throw new IllegalStateException("db password is hunter2");
        }

        @GetMapping("/test/unauthenticated")
        void unauthenticated() {
            throw new BadCredentialsException("bad token signature");
        }

        @GetMapping("/test/denied")
        void denied() {
            throw new AccessDeniedException("needs ROLE_ADMIN");
        }

        @GetMapping("/test/typed")
        void typed(@RequestParam UUID trainer) {}
    }

    record Body(@NotBlank String name) {}

    @Test
    void apiException_returnsItsStatusAndCode() throws Exception {
        mvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("seat_taken"))
                .andExpect(jsonPath("$.detail").value("That seat is already taken."));
    }

    @Test
    void apiException_copiesItsExtraPropertiesOntoTheBody() throws Exception {
        mvc.perform(get("/test/extra"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"))
                .andExpect(jsonPath("$.attemptsLeft").value(3));
    }

    @Test
    void invalidBody_returnsValidationFailedWithFieldErrors() throws Exception {
        mvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void wrongTypeParam_returnsValidationFailedWithTheParamAsField() throws Exception {
        mvc.perform(get("/test/typed").param("trainer", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("trainer"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("invalid_value"));
    }

    @Test
    void unexpectedError_returnsGeneric500() throws Exception {
        mvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("internal_error"))
                .andExpect(jsonPath("$.detail").value("Something went wrong."));
    }

    @Test
    void authenticationException_returns401Unauthorized() throws Exception {
        mvc.perform(get("/test/unauthenticated"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("unauthorized"))
                .andExpect(jsonPath("$.detail").value("Please sign in again."));
    }

    @Test
    void accessDeniedException_returns403Forbidden() throws Exception {
        mvc.perform(get("/test/denied"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("forbidden"))
                .andExpect(jsonPath("$.detail").value("You don't have access to this."));
    }

}
