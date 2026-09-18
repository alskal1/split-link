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
    private String currency;
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
