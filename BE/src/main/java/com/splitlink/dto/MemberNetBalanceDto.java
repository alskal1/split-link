package com.splitlink.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberNetBalanceDto {
    private Long memberId;
    private BigDecimal netBalance; // 양수: 돈을 받아야 함, 음수: 돈을 보내야 함
}
