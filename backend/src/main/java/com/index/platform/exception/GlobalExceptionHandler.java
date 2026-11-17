package com.index.platform.exception;

import com.index.platform.service.ErrorAlertService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ErrorAlertService alerts;

    public record ErrorBody(Instant timestamp, int status, String message, Map<String, String> fieldErrors) {}

    private ResponseEntity<ErrorBody> build(HttpStatus s, String msg, Map<String, String> fields) {
        return ResponseEntity.status(s).body(new ErrorBody(Instant.now(), s.value(), msg, fields));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorBody> api(ApiException e) { return build(e.getStatus(), e.getMessage(), null); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorBody> validation(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorBody> malformed(Exception e) { return build(HttpStatus.BAD_REQUEST, "Malformed request", null); }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorBody> tooBig(MaxUploadSizeExceededException e) { return build(HttpStatus.PAYLOAD_TOO_LARGE, "The file is too large (maximum 1 MB)", null); }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorBody> denied(AccessDeniedException e) { return build(HttpStatus.FORBIDDEN, "You do not have access to this resource", null); }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorBody> integrity(DataIntegrityViolationException e) { return build(HttpStatus.CONFLICT, "Data conflict: the record already exists or is in use", null); }

    /** Anything unexpected: log it, tell the admins (at most every 10 minutes) and answer politely. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorBody> unexpected(Exception e) {
        if (e instanceof org.springframework.web.ErrorResponse er) {
            HttpStatus st = HttpStatus.resolve(er.getStatusCode().value());
            return build(st == null ? HttpStatus.INTERNAL_SERVER_ERROR : st, st == null ? "Request failed" : st.getReasonPhrase(), null);
        }
        log.error("Unexpected error", e);
        alerts.report(e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on the server. The Index team has been told.", null);
    }
}
