package com.voyanta.common.exception;
import com.voyanta.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler (@RestControllerAdvice) for all Voyanta REST controllers.
 * It converts ApiException, request validation errors and unexpected exceptions
 * into a consistent ApiResponse body with the right HTTP status.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        // 4xx/5xx that we raised on purpose are logged at their own level by the
        // caller; here we only avoid a duplicate stack trace for 5xx.
        if (ex.getStatus().is5xxServerError()) {
            log.error("Xidmət xətası: code={} message={}", ex.getErrorCode(), ex.getMessage());
        }
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .errorCode("VALIDATION_FAILED")
                        .data(fieldErrors)
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        // ƏVVƏL BU İSTİSNA HEÇ NERƏ LOGLANMIRDILDI — stack trace itibən itirdi.
        // Məhz bu səbəbdə production-da "giriş 401 oldu" xətasının əsl səbəbini
        // (hansı xarici servisin, hansı xətanın fail olduğu) tapa bilmədik.
        // İndi səbəb loglanır, cavab isə eynidir.
        log.error("Gözlənilməz xəta baş verdi: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "Gözlənilməz xəta baş verdi"));
    }
}
