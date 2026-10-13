package com.splitlink.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@NoArgsConstructor
public class FrankfurterApiResponse {
    private BigDecimal amount;
    private String base;
    private String date;
    private Map<String, BigDecimal> rates;
}
