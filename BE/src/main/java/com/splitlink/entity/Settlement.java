package com.splitlink.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 최종 정산 송금 목록(settlements) 테이블과 매핑되는 Entity 클래스
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Settlement {
    private Long settlementId;
    private Long roomId;
    private Long senderId;
    private Long receiverId;
    private BigDecimal amount;
    private boolean isDone;
}
