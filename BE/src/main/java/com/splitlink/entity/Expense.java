package com.splitlink.entity;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 지출(expenses) 테이블과 매핑되는 Entity 클래스
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expense {
    private Long expenseId;
    private Long roomId;
    private Long payerId;
    private String title;
    private BigDecimal amount;
    private String currency;
    private BigDecimal fxRate;
    private LocalDateTime spentAt;
}
