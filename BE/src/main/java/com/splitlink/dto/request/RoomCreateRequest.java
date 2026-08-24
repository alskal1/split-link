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
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomCreateRequest {
    @NotBlank(message = "방 제목은 필수입니다.") // 필수 값, 공백 금지
    private String title;

    @NotBlank(message = "기준 통화는 필수입니다.")
    @Size(min = 3, max = 3, message = "기준 통화는 3글자여야 합니다.") // 예: KRW
    private String baseCurrency;

    @NotBlank(message = "핀 번호는 필수입니다.")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,10}$", message = "입장코드는 영대소문자와 숫자 조합으로 4~10자리여야 합니다.")
    private String pin;

    @NotEmpty(message = "최소 한 명 이상의 멤버가 필요합니다.")
    private List<String> memberNames;
}
