package com.splitlink.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 지출 목록 및 상단 정산 요약 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseListResponse {

    /** 방 제목 */
    private String roomTitle;

    /** 현재 접속한 사용자 이름 */
    private String currentMemberName;

    /** 방 정산 마감 여부 (true: 정산 완료, false: 지출 입력 중) */
    @Getter(onMethod_ = {@JsonProperty("isLocked")})
    private boolean isLocked;

    /** 방 전체 총 지출 금액 */
    private BigDecimal totalExpenseAmount;

    /**
     * 로그인한 사용자의 정산 상태
     * - isLocked가 false인 경우: PENDING (정산 진행 중)
     * - isLocked가 true인 경우: RECEIVE(받을 금액), SEND(보낼 금액), ZERO(0원)
     */
    private SettlementStatus settlementStatus;

    /**
     * 로그인한 사용자의 정산 필요 금액
     * - isLocked가 false인 경우 null
     * - isLocked가 true인 경우 최종 정산 계산 금액
     */
    private BigDecimal mySettlementAmount;

    /** 등록된 지출 목록 */
    private List<ExpenseItemResponse> expenses;

    /**
     * 정산 상태 구분
     */
    public enum SettlementStatus {
        RECEIVE,   // 받을 금액 있음
        SEND,      // 보낼 금액 있음
        ZERO,      // 정산 금액 0원
        PENDING    // 정산 미마감 상태
    }

    /**
     * 지출 내역 목록 표시용 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseItemResponse {

        /** 지출 PK */
        private Long expenseId;

        /** 지출 항목명 */
        private String title;

        /** 결제 금액 */
        private BigDecimal amount;

        /** 결제자 이름 */
        private String payerName;

        /** 현재 접속자가 해당 지출의 결제자인지 여부 */
        @Getter(onMethod_ = {@JsonProperty("isMyPayment")})
        private boolean isMyPayment;

        /** 정산 참여 인원 수 */
        private int targetMemberCount;
    }
}