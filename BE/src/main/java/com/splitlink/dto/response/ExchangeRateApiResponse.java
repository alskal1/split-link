package com.splitlink.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * ExchangeRate-API의 과거 일자 조회 엔드포인트 JSON 규격에 대응하는 DTO
 */
@Getter
@NoArgsConstructor
public class ExchangeRateApiResponse {

    private String result; // "success" 또는 "error"

    @JsonProperty("base_code")
    private String baseCode;

    @JsonProperty("conversion_rates")
    private Map<String, BigDecimal> conversionRates;
}
