package com.splitlink.common.exception;

import lombok.Builder;
import lombok.Getter;
import org.springframework.validation.FieldError;

/**
 * DTO 유효성 검증(@Valid) 실패 시 필드별 에러 상세 정보를 담는 DTO
 */
@Getter
@Builder
public class CustomFieldError {

    /** 에러가 발생한 필드명 */
    private String field;

    /** 에러 원인 메세지 (예: "방 제목은 필수입니다.") */
    private String reason;

    /** 클라이언트가 입력한 거절된 값 */
    private String rejectedValue;

    /**
     * Spring Validation의 FieldError 객체를 CustomFieldError 객체로 변환
     *
     * @param fieldError Spring Validation에서 발생한 필드 에러 객체
     * @return 변환된 CustomFieldError 객체
     */
    public static CustomFieldError of(FieldError fieldError) {
        return CustomFieldError.builder()
                .field(fieldError.getField())
                .reason(fieldError.getDefaultMessage())
                .rejectedValue(fieldError.getRejectedValue() != null ? fieldError.getRejectedValue().toString() : "")
                .build();
    }
}
