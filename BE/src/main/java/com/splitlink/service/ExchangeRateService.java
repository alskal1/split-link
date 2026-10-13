package com.splitlink.service;

import com.splitlink.dto.response.ExchangeRateApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 과거 시점 환율 조회 및 캐싱하는 ExchangeRate-API 연동 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final RestClient restClient;

    @Value("${app.exchange-rate.api-key}")
    private String apiKey;

    /**
     * 특정 결제 일자(spentDate) 기준 [지출 통화 -> 방 기준 통화] 환율 조회
     *
     * @param baseCurrency   지출 결제 통화 (예: "JPY", "USD")
     * @param targetCurrency 방 기준 통화 (예: "KRW")
     * @param spentDate      지출 일자 (LocalDate)
     * @return 환율 (BigDecimal)
     */
    @Cacheable(value = "historicalExchangeRates", key = "#baseCurrency + '_' + #targetCurrency + '_' + #spentDate.toString()")
    public BigDecimal getHistoricalExchangeRate(String baseCurrency, String targetCurrency, LocalDate spentDate) {

        // 기준 통화와 대상 통화가 동일한 경우 API 호출 없이 1.0 반환
        if (baseCurrency.equalsIgnoreCase(targetCurrency)) {
            return BigDecimal.ONE;
        }

        try {
            // ExchangeRate-API Historical Endpoint URL 조립
            String url = String.format("https://v6.exchangerate-api.com/v6/%s/history/%s/%d/%d/%d",
                    apiKey,
                    baseCurrency.toUpperCase(),
                    spentDate.getYear(),
                    spentDate.getMonthValue(),
                    spentDate.getDayOfMonth()
            );

            ExchangeRateApiResponse response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(ExchangeRateApiResponse.class);

            if (response != null
                && "success".equalsIgnoreCase(response.getResult())
                && response.getConversionRates() != null) {

                BigDecimal rate = response.getConversionRates().get(targetCurrency.toUpperCase());

                if (rate != null) {
                    log.info("[환율 조회 성공] 날짜: {}, 통화: {}, -> {}, 적용 환율: {}",
                            spentDate, baseCurrency, targetCurrency, rate);
                    return rate;
                }
            }
        } catch (Exception e) {
            log.error("ExchangeRate-API 호출 중 오류 발생 [date: {}, base: {}, target: {}]", spentDate, baseCurrency, targetCurrency, e);
        }

        throw new IllegalArgumentException("해당 날짜(" + spentDate + ")의 환율 정보를 조회할 수 없습니다.");
    }
}
