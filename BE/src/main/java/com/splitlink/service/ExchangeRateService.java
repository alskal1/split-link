package com.splitlink.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitlink.dto.response.FrankfurterApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Cacheable(value = "historicalExchangeRates", key = "#paymentCurrency + '_' + #roomBaseCurrency + '_' + #spentDate.toString()")
    public BigDecimal getHistoricalExchangeRate(String paymentCurrency, String roomBaseCurrency, LocalDate spentDate) {

        // 1. 동일 통화 처리
        if (paymentCurrency.equalsIgnoreCase(roomBaseCurrency)) {
            return BigDecimal.ONE;
        }

        LocalDate europeToday = LocalDate.now(ZoneId.of("Europe/Berlin"));
        String datePath = spentDate.isBefore(europeToday) ? spentDate.toString() : "latest";

        // 1차 시도 (요청 날짜 또는 latest)
        BigDecimal rate = fetchRateFromApi(datePath, paymentCurrency, roomBaseCurrency);
        if (rate != null) {
            // TODO: 환율조회 성공이라고 뜨는데 10/20 조회했지만 latest주소이며 10/19 데이터임. 하지만 로그 출력은 20일이라고 뜸
            log.info("[환율 조회 성공] 요청일: {}, 사용 Endpoint: {}, 환율: {}", spentDate, datePath, rate);
            return rate;
        }

        // 2차 시도 (1차 실패 시 /latest 로 자동 우회 - 주말/공휴일/시차/HTML 404 대응)
        log.warn("[환율 특정일자 조회 실패/HTML응답 -> 최신 환율(/latest)로 우회] 요청일: {}", spentDate);
        BigDecimal fallbackRate = fetchRateFromApi("latest", paymentCurrency, roomBaseCurrency);
        if (fallbackRate != null) {
            log.info("[최신 환율(/latest) 우회 성공] 요청일: {}, 환율: {}", spentDate, fallbackRate);
            return fallbackRate;
        }

        throw new IllegalArgumentException("해당 날짜(" + spentDate + ")의 환율 정보를 가져올 수 없습니다.");
    }

    /**
     * Frankfurter API 호출 및 안전한 JSON 파싱
     */
    private BigDecimal fetchRateFromApi(String datePath, String from, String to) {
        // TODO: latest 검색했으나 검색날짜와 입력날짜가 다른 경우 처리 필요
        String url = String.format("https://api.frankfurter.dev/v1/%s?from=%s&to=%s",
                datePath,
                from.trim().toUpperCase(),
                to.trim().toUpperCase()
        );

        try {
            log.info("[Frankfurter API 호출 시도] URL: {}", url);

            ResponseEntity<String> response = restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    // 4xx, 5xx 에러 발생 시 예외를 던지지 않고 그대로 응답 객체를 받아오도록 설정
                    .onStatus(status -> status.isError(), (req, res) -> {
                        log.warn("[Frankfurter API HTTP 에러 응답] Status: {}, URL: {}", res.getStatusCode(), url);
                    })
                    .toEntity(String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                FrankfurterApiResponse apiResponse = objectMapper.readValue(response.getBody(), FrankfurterApiResponse.class);
                if (apiResponse != null && apiResponse.getRates() != null && apiResponse.getRates().containsKey(to.toUpperCase())) {
                    return apiResponse.getRates().get(to.toUpperCase());
                } else {
                    log.warn("[Frankfurter API Rates 데이터 없음] Response Body: {}", response.getBody());
                }
            } else {
                log.warn("[Frankfurter API 호출 비정상] Status: {}, Body: {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("[Frankfurter API 호출 중 Exception 발생] URL: {}, Error: {}", url, e.getMessage());
        }
        return null;
    }
}