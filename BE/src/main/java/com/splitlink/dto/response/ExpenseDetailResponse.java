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
 * 지출 단건 상세 조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseDetailResponse {

    private Long expenseId;
    private String title;

    // 지출 결제 원본 정보
    private BigDecimal amount;        // 지출 결제 금액
    private String currency;          // 지출 통화 (예: KRW, USD)
    private BigDecimal fxRate;        // 환율

    // 프론트엔드 표시 및 정산 기준용 환산 정보
    private BigDecimal amountInBaseCurrency;  // 방 기준 환산 금액
    private String roomBaseCurrency;          // 방 기준 통화

    private LocalDateTime spentAt;

    // 결제자 및 계좌 정보 (수정 폼 및 정산 참고용)
    private Long payerId;
    private String payerName;
    private String bankName;
    private String accountNumber;

    @Getter(onMethod_ = {@JsonProperty("isMyPayment")})
    private boolean isMyPayment;  // 현재 접속자가 결제자인지 여부

    // 지출 참여자 목록 (부담금 정보)
    private List<TargetMemberDetail> targetMembers;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetMemberDetail {
        private Long memberId;       // 참여자 PK
        private String name;         // 참여자 이름

        /**
         * 개별 최종 부담금 (방 기준 통화/원화 환산 + 1원 오차 보정 완료 금액)
         * -> 토스 송금 딥링크 금액 파라미터로 바로 연결 가능
         */
        private BigDecimal shareAmount;

        @Getter(onMethod_ = {@JsonProperty("isSelf")})
        private boolean isSelf;      // 본인 여부
    }
}
