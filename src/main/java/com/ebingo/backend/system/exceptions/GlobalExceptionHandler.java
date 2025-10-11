package com.ebingo.backend.system.exceptions;

import com.ebingo.backend.common.dto.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.naming.AuthenticationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleRuntimeException(RuntimeException ex, ServerWebExchange exchange) {
        return buildApiResponse(null, HttpStatus.INTERNAL_SERVER_ERROR, ex.toString(), exchange);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleIllegalArgumentException(IllegalArgumentException ex, ServerWebExchange exchange) {
        return buildApiResponse(null, HttpStatus.BAD_REQUEST, ex.getMessage(), exchange);
    }

    @ExceptionHandler(NullPointerException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleNullPointerException(NullPointerException ex, ServerWebExchange exchange) {
        return buildApiResponse(null, HttpStatus.INTERNAL_SERVER_ERROR, "Null pointer exception occurred", exchange);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, ServerWebExchange exchange) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return buildApiResponse(errors, HttpStatus.BAD_REQUEST, "Validation failed", exchange);
    }


    @ExceptionHandler(DuplicateUserException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleDuplicateUserException(DuplicateUserException ex, ServerWebExchange exchange) {
        return buildApiResponse(null, HttpStatus.CONFLICT, ex.getMessage(), exchange);
    }

    @ExceptionHandler(DataIntegrityException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleDataIntegrityException(DataIntegrityException ex, ServerWebExchange exchange) {
        return buildApiResponse(null, HttpStatus.CONFLICT, ex.getMessage(), exchange);
    }

    @ExceptionHandler(UserCreationException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleUserCreationException(UserCreationException ex, ServerWebExchange exchange) {
        String rootMessage = getRootCauseMessage(ex);

        HttpStatus status = (rootMessage.contains("uq_") || rootMessage.contains("unique constraint"))
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;

        return buildApiResponse(null, status, rootMessage, exchange);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleResourceNotFoundException(ResourceNotFoundException ex, ServerWebExchange exchange) {
        return buildApiResponse(null, HttpStatus.NOT_FOUND, ex.getMessage(), exchange);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleAuthorizationDeniedException(AuthorizationDeniedException ex, ServerWebExchange exchange) {
        return buildApiResponse(null, HttpStatus.FORBIDDEN, ex.getMessage(), exchange);
    }


    @ExceptionHandler(AuthenticationException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleAuthenticationException(AuthorizationDeniedException ex, ServerWebExchange exchange) {
        return buildApiResponse(null, HttpStatus.UNAUTHORIZED, ex.getMessage(), exchange);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleDataIntegrityViolationException(DataIntegrityViolationException ex, ServerWebExchange exchange) {
        String rootMessage = getRootCauseMessage(ex);
        HttpStatus status = (rootMessage.contains("uq_") || rootMessage.contains("duplicate"))
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;

        return buildApiResponse(null, status, rootMessage, exchange);
    }

    private Mono<ResponseEntity<ApiResponse<Object>>> buildApiResponse(Map<String, String> errors, HttpStatus status, String message, ServerWebExchange exchange) {
        ApiResponse<Object> apiResponse = ApiResponse.builder()
                .statusCode(status.value())
                .error(status.getReasonPhrase())
                .errors(errors)
                .message(message)
                .path(exchange.getRequest().getPath().value())
                .success(false)
                .timestamp(Instant.now())
                .build();

        return Mono.just(ResponseEntity.status(status).body(apiResponse));
    }
}
