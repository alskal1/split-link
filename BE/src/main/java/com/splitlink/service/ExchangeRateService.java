package com.splitlink.service;

import com.splitlink.dto.response.FrankfurterApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * 특정 결제 일자(spentDate) 기준 [지출 통화 -> 방 기준 통화] fx_rate 조회
     */
    @Cacheable(value = "historicalExchangeRates", key = "#paymentCurrency + '_' + #roomBaseCurrency + '_' + #spentDate.toString()")
    public BigDecimal getHistoricalExchangeRate(String paymentCurrency, String roomBaseCurrency, LocalDate spentDate) {

        // 기준 통화와 대상 통화가 동일한 경우 API 호출 없이 1.0 반환
        if (paymentCurrency.equalsIgnoreCase(roomBaseCurrency)) {
            return BigDecimal.ONE;
        }

        try {
            // ExchangeRate-API Historical Endpoint URL 조립
            String url = String.format("https://api.frankfurter.app/%s?from=%s&to=%s",
                    spentDate.toString(),
                    paymentCurrency.toUpperCase(),
                    roomBaseCurrency.toUpperCase()
            );

            FrankfurterApiResponse response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(FrankfurterApiResponse.class);

            if (response != null && response.getRates() != null) {
                BigDecimal fxRate = response.getRates().get(roomBaseCurrency.toUpperCase());

                if (fxRate != null) {
                    log.info("[환율 조회 성공] 날짜: {}, 통화: {}, -> {}, 적용 환율: {}",
                            spentDate, paymentCurrency, roomBaseCurrency, fxRate);
                    return fxRate;
                }
            }
        } catch (Exception e) {
            log.error("Frankfurter API 환율 조회 실패 [date: {}, payment: {}, roomBase: {}]", spentDate, paymentCurrency, roomBaseCurrency, e);
        }

        throw new IllegalArgumentException("해당 날짜(" + spentDate + ")의 환율(fx_rate) 정보를 가져올 수 없습니다.");
    }
}
