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

    @NotBlank(message = "정산받을 은행명을 입력해 주세요.")
    @Pattern(
            regexp = "^[가-힣a-zA-Z\\s]{2,20}$",
            message = "은행명은 2~20자의 한글 또는 영문만 입력할 수 있습니다."
    )
    private String bankName;

    @NotBlank(message = "정산받을 계좌번호를 입력해 주세요.")
    @Pattern(
            regexp = "^[0-9 -]*[0-9][0-9 -]*$",
            message = "계좌번호는 숫자, 하이픈(-), 공백만 입력할 수 있으며 최소 1개 이상의 숫자가 포함되어야 합니다."
    )
    private String accountNumber;

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
