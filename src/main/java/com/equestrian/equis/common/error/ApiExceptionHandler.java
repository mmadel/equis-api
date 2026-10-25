package com.equestrian.equis.common.error;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import tools.jackson.databind.DatabindException;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ProblemDetail handleApi(ApiException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        ex.properties().forEach(pd::setProperty);
        pd.setProperty("code", ex.code());
        return pd;
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleAuthentication(AuthenticationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Please sign in again.");
        pd.setProperty("code", ErrorCodes.UNAUTHORIZED);
        return pd;
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "You don't have access to this.");
        pd.setProperty("code", ErrorCodes.FORBIDDEN);
        return pd;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = ex.getBody();
        pd.setDetail("Some fields are not valid.");
        pd.setProperty("code", ErrorCodes.VALIDATION_FAILED);
        pd.setProperty("fieldErrors", ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiFieldError(f.getField(), f.getDefaultMessage()))
                .toList());
        return handleExceptionInternal(ex, pd, headers, status, request);
    }

    // A path variable or query parameter of the wrong type, e.g. ?trainer=abc for a UUID.
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String field = ex instanceof MethodArgumentTypeMismatchException m ? m.getName() : ex.getPropertyName();
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Some fields are not valid.");
        pd.setProperty("code", ErrorCodes.VALIDATION_FAILED);
        pd.setProperty("fieldErrors", List.of(new ApiFieldError(field, ErrorCodes.INVALID_VALUE)));
        return handleExceptionInternal(ex, pd, headers, HttpStatus.BAD_REQUEST, request);
    }

    // A body value Jackson can't read into its field, e.g. "startMonth": "Nov" for a YearMonth. Answered like a
    // failed @Valid on that field. A body that isn't JSON at all keeps Spring's default 400.
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (!(ex.getCause() instanceof DatabindException cause) || cause.getPath().isEmpty()) {
            return super.handleHttpMessageNotReadable(ex, headers, status, request);
        }
        String field = cause.getPath().stream()
                .map(ref -> ref.getPropertyName() != null ? ref.getPropertyName() : String.valueOf(ref.getIndex()))
                .collect(Collectors.joining("."));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Some fields are not valid.");
        pd.setProperty("code", ErrorCodes.VALIDATION_FAILED);
        pd.setProperty("fieldErrors", List.of(new ApiFieldError(field, ErrorCodes.INVALID_VALUE)));
        return handleExceptionInternal(ex, pd, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        logger.error("Unhandled error", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong.");
        pd.setProperty("code", ErrorCodes.INTERNAL_ERROR);
        return pd;
    }
}
