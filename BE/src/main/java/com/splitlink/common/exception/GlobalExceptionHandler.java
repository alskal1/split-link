package com.splitlink.common.exception;

import com.splitlink.common.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * 전역 예외 처리를 담당하는 컨트롤러 어드바이스
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직 및 입력값 관련 예외 처리 (HTTP 400 Bad Request)
     *
     * @param e IllegalArgumentException 객체
     * @return 400 상태 코드와 에러 메세지를 담은 ApiResponse
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Business Exception: {}", e.getMessage());

        ApiResponse<Void> response = ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 권한 및 보안 관련 예외 처리 (HTTP 403 Forbidden)
     *
     * @param e SecurityException 객체
     * @return 403 상태 코드와 에러 메세지를 담은 ApiResponse
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurityException(SecurityException e) {
        log.warn("Security Exception: {}", e.getMessage());

        ApiResponse<Void> response = ApiResponse.error(HttpStatus.FORBIDDEN.value(), e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * DTO 유효성 검증(@Valid) 실패 예외 처리 (HTTP 400 Bad Request)
     *
     * @param e MethodArgumentNotValidException 객체
     * @return 400 상태 코드와 필드별 에러 상세 목록(CustomFieldError)을 담은 ApiResponse
     */
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

    /**
     * 서서 내부 예기치 못한 최상위 예외 처리 (HTTP 500 Internal Server Error)
     *
     * @param e Exception 객체
     * @return 500 상태 코드와 공통 에러 메시지를 담은 ApiResponse
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllException(Exception e) {
        log.error("Unexpected Server Error: {}", e.getMessage());

        ApiResponse<Void> response = ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부 오류가 발생했습니다.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
