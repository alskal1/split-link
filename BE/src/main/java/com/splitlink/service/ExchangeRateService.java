package com.splitlink.service;

import com.splitlink.dto.response.FrankfurterApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

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

        // 유럽 현지 시각(CET) 기준 오늘 날짜 구하기
        LocalDate europeToday = LocalDate.now(ZoneId.of("Europe/Berlin"));

        // 지출일이 유럽 현지 기준 오늘이거나 미래면 아직 공시 전이므로 "latest"로 URL 설정, 과거 날짜면 해당 날짜 문자열 사용
        String datePath = spentDate.isBefore(europeToday) ? spentDate.toString() : "latest";

        try {
            // 1차 시도: 계산된 datePath로 호출
            // ExchangeRate-API Historical Endpoint URL 조립
            String url = String.format("https://api.frankfurter.app/%s?from=%s&to=%s",
                    datePath,
                    paymentCurrency.toUpperCase(),
                    roomBaseCurrency.toUpperCase()
            );

            FrankfurterApiResponse response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(FrankfurterApiResponse.class);

            if (response != null && response.getRates() != null && response.getRates().containsKey(roomBaseCurrency.toUpperCase())) {
                BigDecimal fxRate = response.getRates().get(roomBaseCurrency.toUpperCase());
                log.info("[환율 조회 성공] 요청일: {}, 사용 Endpoint: {}, 환율: {}", spentDate, datePath, fxRate);
                return fxRate;
            }

            // 2차 시도 (주말/공휴일 등으로 1차 시도 일자 데이터가 없을 경우): /latest 로 자동 우회
            log.warn("[환율 특정일자 미공시 -> 최신 환율(/latest]로 우회] 요청일: {}", spentDate);
            String fallbackUrl = String.format("https://api.frankfurter.app/latest?from=%s&to=%s",
                    paymentCurrency.toUpperCase(),
                    roomBaseCurrency.toUpperCase()
            );

            FrankfurterApiResponse fallbackResponse = restClient.get()
                    .uri(fallbackUrl)
                    .retrieve()
                    .body(FrankfurterApiResponse.class);

            if (fallbackResponse != null && fallbackResponse.getRates() != null) {
                BigDecimal fxRate = fallbackResponse.getRates().get(roomBaseCurrency.toUpperCase());
                if (fxRate != null) {
                    return fxRate;
                }
            }

        } catch (Exception e) {
            log.error("Frankfurter API 환율 조회 중 예외 발생 [spentDate: {}]", spentDate, e);
        }

        throw new IllegalArgumentException("해당 날짜(" + spentDate + ")의 환율 정보를 가져올 수 없습니다.");
    }
}
