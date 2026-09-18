package com.splitlink.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 지출 일괄 등록 요청 DTO (최상위)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseBatchCreateRequest {

    @Valid
    @NotEmpty(message = "최소 1개 이상의 지출 정보가 필요합니다.")
    private List<ExpenseGroupRequest> expenseGroups;

    /**
     * [결제자 · 결제일시 · 통화 · 계좌] 그룹 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseGroupRequest {

        /** 결제자 PK */
        @NotNull(message = "결제자를 선택해 주세요.")
        private Long payerId;

        /** 결제 일시 */
        @NotNull(message = "결제 일시를 입력해 주세요.")
        private LocalDateTime spentAt;

        /**
         * 결제 통화 (해외결제 미체크 시 null로 전송될 수 있음)
         * 값이 들어올 경우에만 3자리(ISO 4217) 검증 수행
         */
        @Size(min = 3, max = 3, message = "통화 코드는 3글자여야 합니다.")
        private String currency;

        /** 정산 계좌 - 은행명 */
        @NotBlank(message = "정산받을 은행명을 입력해 주세요.")
        private String bankName;

        /** 정산 계좌 - 계좌번호 */
        @NotBlank(message = "정산받을 계좌번호를 입력해 주세요.")
        private String accountNumber;

        /** 해당 그룹에 속한 세부 지출 항목 목록 */
        @Valid
        @NotEmpty(message = "최소 1개 이상의 항목을 입력해 주세요.")
        private List<ExpenseItemRequest> items;
    }

    /**
     * [항목명 · 금액 · 참여자] 세부 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseItemRequest {

        /** 지출 항목명 */
        @NotBlank(message = "항목명을 입력해 주세요.")
        private String title;

        /** 결제 금액 (원화 또는 외화 금액) */
        @NotNull(message = "금액을 입력해 주세요.")
        @Min(value = 1, message = "금액은 1 이상이어야 합니다.")
        private BigDecimal amount;

        /** 함께 정산할 참여 멤버 ID 목록 */
        @NotEmpty(message = "참여자를 1명 이상 선택해 주세요.")
        private List<Long> targetMemberIds;

        /**
         * targetMemberIds 내 중복 ID 존재 여부 검증
         */
        @AssertTrue(message = "참여자 목록에 중복된 멤버가 존재합니다.")
        public boolean isValidTargetMemberIds() {
            if (targetMemberIds == null || targetMemberIds.isEmpty()) {
                return true; // @NotEmpty에서 이미 걸러짐
            }
            return targetMemberIds.size() == new java.util.HashSet<>(targetMemberIds).size();
        }
    }
}
