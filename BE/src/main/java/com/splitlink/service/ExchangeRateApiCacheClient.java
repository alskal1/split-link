package com.splitlink.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitlink.dto.response.FrankfurterApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateApiCacheClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    /**
     * 특정 일자의 정상적인 환율 응답만 캐시에 저장 (Cache Hits / Misses 처리)
     */
    @Cacheable(value = "historicalExchangeRates", key = "#from + '_' + #to + '_' + #cacheKeyDate")
    public BigDecimal fetchHistoricalRate(String datePath, String cacheKeyDate, String from, String to) {
        log.info("========== [CACHE MISS] 캐시에 데이터가 없어 실제 API 호출 진행 -> DatePath: {}, KeyDate: {}, From: {}, To: {} ==========",
                datePath, cacheKeyDate, from, to);
        return fetchRateFromApi(datePath, from, to);
    }

    /**
     * Frankfurter API 실제 HTTP 호출 및 JSON 파싱 (캐시 어노테이션 없음)
     */
    public BigDecimal fetchRateFromApi(String datePath, String from, String to) {
        String url = String.format("https://api.frankfurter.dev/v1/%s?from=%s&to=%s",
                datePath,
                from.trim().toUpperCase(),
                to.trim().toUpperCase());

        try {
            log.info("[Frankfurter API 호출 시도] URL: {}", url);

            ResponseEntity<String> response = restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
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
