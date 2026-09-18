package com.splitlink.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

/**
 * 지출 단건 수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseUpdateRequest {

    @NotNull(message = "결제자를 선택해 주세요.")
    private Long payerId;

    @NotBlank(message = "항목명을 입력해 주세요.")
    private String title;

    @NotNull(message = "금액을 입력해 주세요.")
    @Min(value = 1, message = "금액은 1 이상이어야 합니다.")
    private BigDecimal amount;

    @NotNull(message = "결제 일시를 입력해 주세요.")
    private LocalDateTime spentAt;

    @NotEmpty(message = "참여자를 1명 이상 선택해 주세요.")
    private List<Long> targetMemberIds;

    /**
     * targetMemberIds 내 중복 ID 존재 여부 검증
     */
    @AssertTrue(message = "참여자 목록에 중복된 멤버가 존재합니다.")
    public boolean isValidTargetMemberIds() {
        if (targetMemberIds == null || targetMemberIds.isEmpty()) {
            return true;
        }
        return targetMemberIds.size() == new HashSet<>(targetMemberIds).size();
    }
}
