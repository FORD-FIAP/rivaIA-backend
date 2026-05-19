package com.ford.riva.exception;

import com.ford.riva.dto.error.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String GENERIC_ERROR_MESSAGE = "Erro interno do servidor";
    private static final String GENERIC_BAD_REQUEST_MESSAGE = "Requisição inválida";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(
                err -> errors.put(err.getField(), err.getDefaultMessage())
        );
        log.warn("Validação falhou em {}: {}", request.getRequestURI(), errors);
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Erro de validação nos campos enviados",
                request.getRequestURI(),
                errors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            errors.put(v.getPropertyPath().toString(), v.getMessage());
        }
        log.warn("ConstraintViolation em {}: {}", request.getRequestURI(), errors);
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Erro de validação nos campos enviados",
                request.getRequestURI(),
                errors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedBody(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Body malformado em {}: {}",
                request.getRequestURI(),
                ex.getMostSpecificCause().getClass().getSimpleName());
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Tipo inválido em {}: parâmetro '{}'", request.getRequestURI(), ex.getName());
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Tipo de parâmetro inválido",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Argumento inválido em {}: {}", request.getRequestURI(), ex.getMessage());
        String message = ex.getMessage() != null ? ex.getMessage() : GENERIC_BAD_REQUEST_MESSAGE;
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Autenticação negada em {}: {}",
                request.getRequestURI(), ex.getClass().getSimpleName());
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Credenciais inválidas",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Falha de autenticação em {}: {}",
                request.getRequestURI(), ex.getClass().getSimpleName());
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Autenticação requerida",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acesso negado em {}", request.getRequestURI());
        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Acesso negado",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("Endpoint inexistente acessado: {} {} (possível scan)",
                ex.getHttpMethod(), ex.getRequestURL());
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Recurso não encontrado",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("Método HTTP inválido em {}: {}", request.getRequestURI(), ex.getMethod());
        return buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Método HTTP não suportado",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimit(
            RateLimitExceededException ex, HttpServletRequest request) {
        log.warn("Rate limit excedido em {}", request.getRequestURI());
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                .message("Limite de requisições excedido. Tente novamente mais tarde.")
                .path(request.getRequestURI())
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()));
        return new ResponseEntity<>(body, headers, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Erro interno em {}: ", request.getRequestURI(), ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                GENERIC_ERROR_MESSAGE,
                request.getRequestURI(),
                null
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status, String message, String path, Map<String, String> errors) {
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .errors(errors)
                .build();
        return new ResponseEntity<>(body, status);
    }
}
