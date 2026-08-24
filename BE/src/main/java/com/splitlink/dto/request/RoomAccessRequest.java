package com.splitlink.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 방 입장코드 검증 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomAccessRequest {

    /** 입장코드 PIN 번호 */
    @NotBlank(message = "입장코드를 입력해 주세요.")
    private String pin;
}
