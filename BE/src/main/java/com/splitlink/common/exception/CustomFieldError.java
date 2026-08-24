package com.splitlink.common.exception;

import lombok.Builder;
import lombok.Getter;
import org.springframework.validation.FieldError;

@Getter
@Builder
public class CustomFieldError {
    private String field;            // 에러가 발생한 필드명
    private String reason;           // 에러 이유 (ex. "방 제목은 필수입니다.")
    private String rejectedValue;    // 잘못 들어온 값

    public static CustomFieldError of(FieldError fieldError) {
        return CustomFieldError.builder()
                .field(fieldError.getField())
                .reason(fieldError.getDefaultMessage())
                .rejectedValue(fieldError.getRejectedValue() != null ? fieldError.getRejectedValue().toString() : "")
                .build();
    }
}
