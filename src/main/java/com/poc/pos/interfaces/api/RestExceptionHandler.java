package com.poc.pos.interfaces.api;

import com.poc.pos.domain.exception.IdempotencyConflictException;
import com.poc.pos.domain.exception.InvalidTransactionStateException;
import com.poc.pos.domain.exception.TransactionNotFoundException;
import com.poc.pos.interfaces.api.dto.ErrorResponse;
import com.poc.pos.security.HmacAuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(TransactionNotFoundException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler({
            InvalidTransactionStateException.class,
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception exception, HttpServletRequest request) {
        String message = exception instanceof MethodArgumentNotValidException
                ? "Invalid request payload"
                : exception.getMessage();
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message, request);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(IdempotencyConflictException exception, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", exception.getMessage(), request);
    }

    @ExceptionHandler(HmacAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(HmacAuthenticationException exception, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_HMAC", exception.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error", request);
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(code, message, request.getHeader("X-Correlation-Id")));
    }
}
