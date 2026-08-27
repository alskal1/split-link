package com.splitlink.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomUpdateRequest {
    /** 방 제목 */
    @NotBlank(message = "방 제목은 필수입니다.") // 필수 값, 공백 금지
    private String title;

    /** 기준 통화 (ISO 4217 3자리 코드, 예: KRW, USD) */
    @NotBlank(message = "기준 통화는 필수입니다.")
    @Size(min = 3, max = 3, message = "기준 통화는 3글자여야 합니다.") // 예: KRW
    private String baseCurrency;

    /** 기존 입장코드 PIN 번호 (권한 확인용 필수값) */
    @NotBlank(message = "핀 번호는 필수입니다.")
    private String pin;

    /** 새로운 입장코드 PIN 번호 (선택값: 입력 시 변경, 미입력 시 유지) */
    private String newPin;

    /** 참여 멤버 이름 목록 */
    @NotEmpty(message = "최소 한 명 이상의 멤버가 필요합니다.")
    private List<String> memberNames;
}
