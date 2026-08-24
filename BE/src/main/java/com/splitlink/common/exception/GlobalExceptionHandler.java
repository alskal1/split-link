package com.splitlink.common.exception;

import com.splitlink.common.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 잘못된 요청 (400)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Business Exception: {}", e.getMessage());

        ApiResponse<Void> response = ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 2. 권한 없음 (403)
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurityException(SecurityException e) {
        log.warn("Security Exception: {}", e.getMessage());

        ApiResponse<Void> response = ApiResponse.error(HttpStatus.FORBIDDEN.value(), e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // 3. 유효성 검사 오류 (자동 검증 실패)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<CustomFieldError>>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Validation Exception: {}", e.getMessage());

        List<CustomFieldError> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(CustomFieldError::of)
                .toList();

        ApiResponse<List<CustomFieldError>> response = ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                "입력값 유효성 검증에 실패했습니다.",
                fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 4. 서버 내부 오류
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllException(Exception e) {
        log.error("Unexpected Server Error: {}", e.getMessage());

        ApiResponse<Void> response = ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부 오류가 발생했습니다.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
