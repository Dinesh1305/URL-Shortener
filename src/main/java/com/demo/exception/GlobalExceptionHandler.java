package com.demo.exception;

import com.demo.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUrlNotFound(UrlNotFoundException ex, HttpServletRequest request) {
        log.warn("URL not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UrlExpiredException.class)
    public ResponseEntity<ErrorResponse> handleUrlExpired(UrlExpiredException ex, HttpServletRequest request) {
        log.warn("URL expired: {}", ex.getMessage());
        return buildResponse(HttpStatus.GONE, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DuplicateAliasException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateAlias(DuplicateAliasException ex, HttpServletRequest request) {
        log.warn("Duplicate alias conflict: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RateLimitExceededException ex, HttpServletRequest request) {
        log.warn("Rate limit exceeded for path {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidUrlException.class)
    public ResponseEntity<ErrorResponse> handleInvalidUrl(InvalidUrlException ex, HttpServletRequest request) {
        log.warn("Invalid URL requested: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Validation failed for {}: {}", request.getRequestURI(), errorMessage);
        return buildResponse(HttpStatus.BAD_REQUEST, errorMessage, request.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("Data integrity violation: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "Custom alias or short code already exists", request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception occurred at {}: ", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred. Please try again later.", request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, String path) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .message(message)
                .path(path)
                .build();
        return new ResponseEntity<>(error, status);
    }
}
