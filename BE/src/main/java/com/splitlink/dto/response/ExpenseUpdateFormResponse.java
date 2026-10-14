package com.splitlink.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 지출 수정 폼 데이터 조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseUpdateFormResponse {

    private Long expenseId;
    private String title;
    private BigDecimal amount;
    private String bankName;
    private String accountNumber;
    
    // 다통화 및 환산 관련 추가 필드
    private String currency;                 // 지출 당시 입력 통화 (예: USD)
    private BigDecimal fxRate;               // 적용된 환율
    private BigDecimal amountInBaseCurrency; // 방 기준 통화 환산 금액 (amount * fx_rate)
    private String roomBaseCurrency;         // 방 기준 통화 (예: KRW)

    private LocalDateTime spentAt;

    // 기존 지정된 결제자 PK 및 참여자 PK 목록
    private Long payerId;
    private List<Long> targetMemberIds;

    // 결제자/참여자 변경 선택창을 위한 방 전체 멤버 목록
    private List<MemberInfo> roomMembers;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberInfo {
        private Long memberId;
        private String name;

        @Getter(onMethod_ = {@JsonProperty("isActive")})
        private boolean isActive;
    }
}
